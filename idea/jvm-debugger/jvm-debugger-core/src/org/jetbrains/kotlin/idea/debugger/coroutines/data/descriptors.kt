/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines.data

import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.impl.descriptors.data.DescriptorData
import com.intellij.debugger.impl.descriptors.data.DisplayKey
import com.intellij.debugger.impl.descriptors.data.SimpleDisplayKey
import com.intellij.debugger.jdi.StackFrameProxyImpl
import com.intellij.debugger.memory.utils.StackFrameItem
import com.intellij.debugger.ui.impl.watch.NodeDescriptorImpl
import com.intellij.openapi.project.Project
import com.sun.jdi.*
import org.jetbrains.kotlin.idea.debugger.coroutines.proxy.LookupContinuation
import org.jetbrains.kotlin.idea.debugger.evaluate.ExecutionContext

class CoroutineStackTraceData(state: CoroutineState, proxy: StackFrameProxyImpl, evalContext: EvaluationContextImpl, val frame: StackTraceElement)
    : CoroutineStackDescriptorData(state, proxy, evalContext) {

    override fun hashCode() = frame.hashCode()

    override fun equals(other: Any?) =
        other is CoroutineStackTraceData && frame == other.frame

    override fun createDescriptorImpl(project: Project): NodeDescriptorImpl {
        val context = ExecutionContext(evalContext, proxy)
        val lookupContinuation = LookupContinuation(context, frame)

        // retrieve continuation only if suspend method
        val continuation = lookupContinuation.findContinuation(state)

        return if (continuation is ObjectReference)
            SuspendStackFrameDescriptor(state, frame, proxy, continuation)
            else
            CoroutineCreatedStackFrameDescriptor(frame, proxy)
    }
}

class CoroutineStackFrameData(state: CoroutineState, proxy: StackFrameProxyImpl, evalContext: EvaluationContextImpl, val frame: StackFrameItem)
    : CoroutineStackDescriptorData(state, proxy, evalContext) {
    override fun createDescriptorImpl(project: Project): NodeDescriptorImpl =
        AsyncStackFrameDescriptor(state, frame, proxy)

    override fun hashCode() = frame.hashCode()

    override fun equals(other: Any?) = other is CoroutineStackFrameData && frame == other.frame
}

abstract class CoroutineStackDescriptorData(val state: CoroutineState, val proxy: StackFrameProxyImpl, val evalContext: EvaluationContextImpl)
    : DescriptorData<NodeDescriptorImpl>() {

    override fun getDisplayKey(): DisplayKey<NodeDescriptorImpl> = SimpleDisplayKey(state)
}

/**
 * Describes coroutine itself in the tree (name: STATE), has children if stacktrace is not empty (state = CREATED)
 */
class CoroutineDescriptorData(private val state: CoroutineState) : DescriptorData<CoroutineDescriptorImpl>() {

    override fun createDescriptorImpl(project: Project) =
        CoroutineDescriptorImpl(state)

    override fun equals(other: Any?) = if (other !is CoroutineDescriptorData) false else state.name == other.state.name

    override fun hashCode() = state.name.hashCode()

    override fun getDisplayKey(): DisplayKey<CoroutineDescriptorImpl> = SimpleDisplayKey(state.name)
}