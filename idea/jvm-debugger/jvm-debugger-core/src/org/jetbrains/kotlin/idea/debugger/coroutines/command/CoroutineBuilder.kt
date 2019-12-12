/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines.command

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.JavaStackFrame
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl
import com.intellij.debugger.memory.utils.StackFrameItem
import com.intellij.debugger.ui.impl.watch.MethodsTracker
import com.intellij.debugger.ui.impl.watch.StackFrameDescriptorImpl
import com.sun.jdi.ThreadReference
import org.jetbrains.kotlin.idea.debugger.coroutines.CoroutineAsyncStackTraceProvider
import org.jetbrains.kotlin.idea.debugger.coroutines.data.CoroutineInfoData


class CoroutineBuilder(val suspendContext: SuspendContextImpl) {
    private val methodsTracker = MethodsTracker()
    private val coroutineStackFrameProvider = CoroutineAsyncStackTraceProvider()

    companion object {
        val CREATION_STACK_TRACE_SEPARATOR = "\b\b\b" // the "\b\b\b" is used for creation stacktrace separator in kotlinx.coroutines
    }

    fun build(ci: CoroutineInfoData): List<CoroutineStackFrameItem> {
        val coroutineStackFrameList = mutableListOf<CoroutineStackFrameItem>()
        val stackFrameProxyImpl = firstSuspendedThreadFrame()
        val creationFrameSeparatorIndex = findCreationFrameIndex(ci.stackTrace)
        val runningThreadReferenceProxyImpl = currentRunningThreadProxy(ci.thread)
        val positionManager = suspendContext.debugProcess.positionManager

        if (ci.state == CoroutineInfoData.State.RUNNING && runningThreadReferenceProxyImpl is ThreadReferenceProxyImpl) {
            val frames = runningThreadReferenceProxyImpl.forceFrames()
            var resumeMethodIndex = findResumeMethodIndex(frames)
            for (frameIndex in 0..frames.lastIndex) {
                val runningStackFrameProxy = frames[frameIndex]
                if (frameIndex == resumeMethodIndex) {
                    val previousFrame = frames[resumeMethodIndex - 1]
                    val previousJavaFrame = JavaStackFrame(StackFrameDescriptorImpl(previousFrame, methodsTracker), true)
                    val async = coroutineStackFrameProvider
                        .getAsyncStackTrace(previousJavaFrame, suspendContext)
                    val sourcePosition = positionManager.getSourcePosition(runningStackFrameProxy.location())
                    coroutineStackFrameList
                        .add(CoroutineStackFrameItem(runningStackFrameProxy, "should be removed", sourcePosition)) // @TODO comment out this line
                    async?.forEach {
                        it.
                        coroutineStackFrameList.add(AsyncCoroutineStackFrameItem(runningStackFrameProxy, "some label", it))
                    }
                } else {
                    coroutineStackFrameList.add(CoroutineStackFrameItem(runningStackFrameProxy))
                }
            }
        } else if (ci.state == CoroutineInfoData.State
                .SUSPENDED || runningThreadReferenceProxyImpl == null
        ) { // to get frames from CoroutineInfo anyway
            // the thread is paused on breakpoint - it has at least one frame
            ci.stackTrace.subList(0, creationFrameSeparatorIndex).forEach {
                coroutineStackFrameList.add(SuspendCoroutineStackFrameItem(stackFrameProxyImpl, "suspended frame", it))
            }
        }

        ci.stackTrace.subList(creationFrameSeparatorIndex + 1, ci.stackTrace.size).forEach {
            coroutineStackFrameList.add(CreationCoroutineStackFrameItem(stackFrameProxyImpl, "creation frame", it))
        }
        ci.stackFrameList.addAll(coroutineStackFrameList)
        return coroutineStackFrameList
    }

    /**
     * Tries to find creation frame separator if any, returns last index if none found
     */
    private fun findCreationFrameIndex(frames: List<StackTraceElement>): Int {
        var index = frames.indexOfFirst { isCreationSeparatorFrame(it) }
        return if (index < 0)
            frames.lastIndex
        else
            index
    }

    private fun isCreationSeparatorFrame(it: StackTraceElement) =
        it.className.startsWith(CREATION_STACK_TRACE_SEPARATOR)

    fun firstSuspendedThreadFrame(): StackFrameProxyImpl =
        suspendedThreadProxy().forceFrames().first()

    // retrieves currently suspended but active and executing corouting thread proxy
    fun currentRunningThreadProxy(threadReference: ThreadReference?): ThreadReferenceProxyImpl? =
        ThreadReferenceProxyImpl(suspendContext.debugProcess.virtualMachineProxy, threadReference)

    // retrieves current suspended thread proxy
    fun suspendedThreadProxy(): ThreadReferenceProxyImpl =
        suspendContext.thread!! // @TODO hash replace !!

    private fun findResumeMethodIndex(frames: List<StackFrameProxyImpl>): Int {
        for (j: Int in frames.lastIndex downTo 0)
            if (isResumeMethodFrame(frames[j])) {
                return j
            }
        return 0
    }

    private fun isResumeMethodFrame(frame: StackFrameProxyImpl) = frame.location().method().name() == "resumeWith"

}

class CreationCoroutineStackFrameItem(
    frame: StackFrameProxyImpl,
    label: String = "",
    val stackTraceElement: StackTraceElement,
    sourcePosition: SourcePosition?
) : CoroutineStackFrameItem(frame, label, sourcePosition)

class SuspendCoroutineStackFrameItem(
    frame: StackFrameProxyImpl,
    label: String = "",
    val stackTraceElement: StackTraceElement,
    sourcePosition: SourcePosition?
) : CoroutineStackFrameItem(frame, label, sourcePosition)

class AsyncCoroutineStackFrameItem(
    frame: StackFrameProxyImpl,
    label: String = "",
    val frameItem: StackFrameItem,
    sourcePosition: SourcePosition?
) : CoroutineStackFrameItem(frame, label, sourcePosition)

open class CoroutineStackFrameItem(val frame: StackFrameProxyImpl, val label: String = "", val sourcePosition: SourcePosition?)