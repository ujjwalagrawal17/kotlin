/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines.view

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.SimpleColoredComponent
import com.intellij.util.SingleAlarm
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.frame.*
import com.intellij.xdebugger.frame.presentation.XRegularValuePresentation
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTree
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTreePanel
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueContainerNode
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.debugger.coroutines.CoroutineDebuggerContentInfo
import org.jetbrains.kotlin.idea.debugger.coroutines.CoroutineDebuggerContentInfo.Companion.XCOROUTINE_POPUP_ACTION_GROUP
import org.jetbrains.kotlin.idea.debugger.coroutines.util.ContentParamProvider
import org.jetbrains.kotlin.idea.debugger.coroutines.util.CreateContentParam
import org.jetbrains.kotlin.idea.debugger.coroutines.util.XDebugSessionListenerProvider
import org.jetbrains.kotlin.idea.debugger.coroutines.util.logger


class XCoroutineThreadView(val project: Project, val session: XDebugSession) :
    Disposable, XDebugSessionListenerProvider, ContentParamProvider {
    val log by logger
    val panel = XDebuggerTreePanel(project, session.debugProcess.editorsProvider, this, null, XCOROUTINE_POPUP_ACTION_GROUP, null)
    val alarm = SingleAlarm(Runnable { clear() },
                            VIEW_CLEAR_DELAY, this)

    companion object {
        private val VIEW_CLEAR_DELAY = 100 //ms
    }

    fun clear() {
        DebuggerUIUtil.invokeLater {
            panel.tree
                .setRoot(object : XValueContainerNode<XValueContainer>(panel.tree, null, true, object : XValueContainer() {}) {}, false)
        }
    }

    override fun dispose() {
    }

    class CoroutineContainer(val suspendContext: XSuspendContext) : XValueContainer() {
        override fun computeChildren(node: XCompositeNode) {
            suspendContext.computeExecutionStacks(object : XSuspendContext.XExecutionStackContainer {
                override fun errorOccurred(errorMessage: String) {

                }

                override fun addExecutionStack(executionStacks: MutableList<out XExecutionStack>, last: Boolean) {
                    val children = XValueChildrenList()
                    executionStacks.map {
                        FramesContainer(it)
                    }.forEach { children.add("", it) }
                    node.addChildren(children, last)
                }
            })
        }
    }

    class FramesContainer(private val executionStack: XExecutionStack) : XValue() {
        override fun computeChildren(node: XCompositeNode) {
            executionStack.computeStackFrames(0, object : XExecutionStack.XStackFrameContainer {
                override fun errorOccurred(errorMessage: String) {
                }

                override fun addStackFrames(stackFrames: MutableList<out XStackFrame>, last: Boolean) {
                    val children = XValueChildrenList()
                    stackFrames.forEach { children.add("",
                                                       FrameValue(it)
                    ) }
                    node.addChildren(children, last)
                }
            })
        }

        override fun computePresentation(node: XValueNode, place: XValuePlace) {
            node.setPresentation(executionStack.icon, XRegularValuePresentation(executionStack.displayName, null, ""), true)
        }
    }

    class FrameValue(val frame: XStackFrame) : XValue() {
        override fun computePresentation(node: XValueNode, place: XValuePlace) {
            val component = SimpleColoredComponent()
            frame.customizePresentation(component)
            node.setPresentation(component.icon, XRegularValuePresentation(component.getCharSequence(false).toString(), null, ""), false)
        }
    }

    class XThreadsRootNode(tree: XDebuggerTree, suspendContext: XSuspendContext) :
        XValueContainerNode<CoroutineContainer>(tree, null, false,
                                                CoroutineContainer(
                                                                                                                                                   suspendContext
                                                                                                                                               )
        )

    fun forceClear() {
        alarm.cancel()
        clear()
    }

    fun createRoot(suspendContext: XSuspendContext) =
        XThreadsRootNode(panel.tree, suspendContext)


    override fun createDebugSessionListener(session: XDebugSession) =
        CoroutineThreadViewDebugSessionListener(session, this)

    override fun provideContentParam(): CreateContentParam =
        CreateContentParam(
            CoroutineDebuggerContentInfo
                .XCOROUTINE_THREADS_CONTENT,
            panel.mainPanel,
            KotlinBundle.message("debugger.session.tab.xcoroutine.title"),
            null,
            panel.tree
        )
}
