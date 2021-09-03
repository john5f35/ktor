/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.server.netty

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.util.*
import io.netty.channel.*
import io.netty.util.*
import kotlinx.atomicfu.*
import kotlinx.coroutines.*

public abstract class NettyApplicationCall(
    application: Application,
    public val context: ChannelHandlerContext,
    private val requestMessage: Any
) : BaseApplicationCall(application) {

    @OptIn(InternalAPI::class)
    public abstract override val request: NettyApplicationRequest
    @OptIn(InternalAPI::class)
    public abstract override val response: NettyApplicationResponse

    public val responseWriteJob: Job = Job()

    private val messageReleased = atomic(false)

    override fun afterFinish(handler: (Throwable?) -> Unit) {
        responseWriteJob.invokeOnCompletion(handler)
    }

    internal suspend fun finish() {
        try {
            @OptIn(InternalAPI::class)
            response.ensureResponseSent()
        } catch (cause: Throwable) {
            finishComplete()
            throw cause
        }

        if (responseWriteJob.isCompleted) {
            finishComplete()
            return
        }

        return finishSuspend()
    }

    private suspend fun finishSuspend() {
        try {
            responseWriteJob.join()
        } finally {
            finishComplete()
        }
    }

    @OptIn(InternalAPI::class)
    private fun finishComplete() {
        responseWriteJob.cancel()
        request.close()
        releaseRequestMessage()
    }

    @OptIn(InternalAPI::class)
    internal fun dispose() {
        response.close()
        request.close()
        releaseRequestMessage()
    }

    private fun releaseRequestMessage() {
        if (messageReleased.compareAndSet(expect = false, update = true)) {
            ReferenceCountUtil.release(requestMessage)
        }
    }
}
