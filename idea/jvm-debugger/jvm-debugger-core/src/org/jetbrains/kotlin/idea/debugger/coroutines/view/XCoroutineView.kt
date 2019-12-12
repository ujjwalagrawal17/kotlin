/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines.view

import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.SuspendContextImpl
import com.intellij.debugger.impl.DebuggerUtilsEx
import com.intellij.debugger.jdi.ThreadReferenceProxyImpl
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.ColoredTextContainer
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.SingleAlarm
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebuggerBundle
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.frame.*
import com.intellij.xdebugger.frame.presentation.XRegularValuePresentation
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTree
import com.intellij.xdebugger.impl.ui.tree.XDebuggerTreePanel
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueContainerNode
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.debugger.coroutines.CoroutineDebuggerContentInfo
import org.jetbrains.kotlin.idea.debugger.coroutines.CoroutineDebuggerContentInfo.Companion.XCOROUTINE_POPUP_ACTION_GROUP
import org.jetbrains.kotlin.idea.debugger.coroutines.command.AsyncCoroutineStackFrameItem
import org.jetbrains.kotlin.idea.debugger.coroutines.command.CoroutineStackFrameItem
import org.jetbrains.kotlin.idea.debugger.coroutines.command.CreationCoroutineStackFrameItem
import org.jetbrains.kotlin.idea.debugger.coroutines.command.SuspendCoroutineStackFrameItem
import org.jetbrains.kotlin.idea.debugger.coroutines.data.CoroutineInfoData
import org.jetbrains.kotlin.idea.debugger.coroutines.proxy.CoroutinesDebugProbesProxy
import org.jetbrains.kotlin.idea.debugger.coroutines.util.CreateContentParams
import org.jetbrains.kotlin.idea.debugger.coroutines.util.CreateContentParamsProvider
import org.jetbrains.kotlin.idea.debugger.coroutines.util.XDebugSessionListenerProvider
import org.jetbrains.kotlin.idea.debugger.coroutines.util.logger
import javax.swing.Icon


class XCoroutineView(val project: Project, val session: XDebugSession) :
    Disposable, XDebugSessionListenerProvider, CreateContentParamsProvider {
    val log by logger
    val panel = XDebuggerTreePanel(project, session.debugProcess.editorsProvider, this, null, XCOROUTINE_POPUP_ACTION_GROUP, null)
    val alarm = SingleAlarm(Runnable { clear() }, VIEW_CLEAR_DELAY, this)
    val debugProcess: DebugProcessImpl = session.debugProcess as DebugProcessImpl

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

    fun forceClear() {
        alarm.cancel()
        clear()
    }

    fun createRoot(suspendContext: XSuspendContext) =
        XCoroutinesRootNode(panel.tree, suspendContext)

    override fun debugSessionListener(session: XDebugSession) =
        CoroutineThreadViewDebugSessionListener(session, this)

    override fun createContentParams(): CreateContentParams =
        CreateContentParams(
            CoroutineDebuggerContentInfo.XCOROUTINE_THREADS_CONTENT,
            panel.mainPanel,
            KotlinBundle.message("debugger.session.tab.xcoroutine.title"),
            null,
            panel.tree
        )

}

class XCoroutinesRootNode(tree: XDebuggerTree, suspendContext: XSuspendContext) :
    XValueContainerNode<CoroutineGroupContainer>(tree, null, false, CoroutineGroupContainer(suspendContext)) {
}

class CoroutineGroupContainer(
    val suspendContext: XSuspendContext
) : XValueContainer() {
    override fun computeChildren(node: XCompositeNode) {
        val children = XValueChildrenList()
        children.add("", CoroutineContainer(suspendContext, "Default group"))
        node.addChildren(children, true)
    }
}

class CoroutineContainer(
    val suspendContext: XSuspendContext,
    val groupName: String
) : XValue() {
    override fun computeChildren(node: XCompositeNode) {
        val debugProbesProxy = CoroutinesDebugProbesProxy(suspendContext as SuspendContextImpl)
        var coroutineCache = debugProbesProxy.dumpCoroutines()
        if(coroutineCache.isOk()) {
            val children = XValueChildrenList()
            coroutineCache.cache.forEach {
                children.add("", FramesContainer(it, debugProbesProxy))
            }
            node.addChildren(children, true)
        } else
            node.addChildren(XValueChildrenList.EMPTY, true)
    }

    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        node.setPresentation(AllIcons.Debugger.ThreadGroup, XRegularValuePresentation(groupName, null, ""), true)
    }
}

class FramesContainer(
    private val coroutineInfoData: CoroutineInfoData,
    private val debugProbesProxy: CoroutinesDebugProbesProxy
) : XValue() {
    override fun computeChildren(node: XCompositeNode) {
        val children = XValueChildrenList()
        debugProbesProxy.frameBuilder().build(coroutineInfoData)
        val creationStack = mutableListOf<CreationCoroutineStackFrameItem>()
        coroutineInfoData.stackFrameList.forEach {
            val frameValue = when (it) {
                is SuspendCoroutineStackFrameItem -> SuspendFrameValue(coroutineInfoData, it)
                is AsyncCoroutineStackFrameItem -> AsyncFrameValue(coroutineInfoData, it)
                is CreationCoroutineStackFrameItem -> {
                    creationStack.add(it)
                    null
                }
                else -> null
            }
            frameValue?.let {
                children.add("", frameValue)
            }
        }
        children.add("", CreationFramesContainer(creationStack))
        node.addChildren(children, true)
    }

    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        val icon = when (coroutineInfoData.state) {
            CoroutineInfoData.State.SUSPENDED -> AllIcons.Debugger.ThreadSuspended
            CoroutineInfoData.State.RUNNING -> AllIcons.Debugger.ThreadRunning
            CoroutineInfoData.State.CREATED -> AllIcons.Debugger.ThreadStates.Idle
        }
        val thread = coroutineInfoData.thread
        val name = thread?.name()?.substringBefore(" @${coroutineInfoData.name}") ?: ""
        val threadState = if (thread != null) DebuggerUtilsEx.getThreadStatusText(thread.status()) else ""
        val text = "${coroutineInfoData.name}: ${coroutineInfoData.state}${if (name.isNotEmpty()) " on thread \"$name\":$threadState" else ""}"
        node.setPresentation(icon, XRegularValuePresentation(text, null, ""), true)
    }
}



class SuspendFrameValue(val coroutineInfoData: CoroutineInfoData, val frame: SuspendCoroutineStackFrameItem) : XValue() {
    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        val component = customizePresentation(SimpleColoredComponent(), frame)
        node.setPresentation(component.icon, XRegularValuePresentation(component.getCharSequence(false).toString(), null, ""), false)
    }
}

class AsyncFrameValue(val coroutineInfoData: CoroutineInfoData, val frame: AsyncCoroutineStackFrameItem) : XValue() {
    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        val component = customizePresentation(SimpleColoredComponent(), frame)
        node.setPresentation(component.icon, XRegularValuePresentation(component.getCharSequence(false).toString(), null, ""), false)
    }
}

fun customizePresentation(component: SimpleColoredComponent, frame: CoroutineStackFrameItem) : SimpleColoredComponent {
    if (position != null) {
        component.append(position.file.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        component.append(":" + (position.line + 1), SimpleTextAttributes.REGULAR_ATTRIBUTES)
        component.setIcon(AllIcons.Debugger.Frame)
    } else {
        component.append(XDebuggerBundle.message("invalid.frame"), SimpleTextAttributes.ERROR_ATTRIBUTES)
    }
    return component
}

class CreationFramesContainer(val creationFrames: List<CreationCoroutineStackFrameItem>) : XValue() {
    override fun computeChildren(node: XCompositeNode) {
        val children = XValueChildrenList()

        creationFrames.forEach {
            children.add("", FrameValue(coroutineInfoData, it))
        }
        children.add("", CreationFramesContainer(coroutineInfoData))
        node.addChildren(children, true)
    }

    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        val component = SimpleColoredComponent()
        frame.customizePresentation(component)
        node.setPresentation(component.icon, XRegularValuePresentation(component.getCharSequence(false).toString(), null, ""), true)
    }
}

fun calcIcon(threadProxy: ThreadReferenceProxyImpl, current: Boolean): Icon? {
    return if (current) {
        if (threadProxy.isSuspended) AllIcons.Debugger.ThreadCurrent else AllIcons.Debugger
            .ThreadRunning
    } else if (threadProxy.isAtBreakpoint) {
        AllIcons.Debugger.ThreadAtBreakpoint
    } else if (threadProxy.isSuspended) {
        AllIcons.Debugger.ThreadSuspended
    } else {
        AllIcons.Debugger.ThreadRunning
    }
}
