/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines.view

import com.intellij.openapi.application.ApplicationManager
import com.intellij.xdebugger.XDebugSession
import com.intellij.xdebugger.XDebugSessionListener
import com.intellij.xdebugger.frame.XSuspendContext
import com.intellij.xdebugger.impl.ui.DebuggerUIUtil
import org.jetbrains.kotlin.idea.debugger.coroutines.util.logger
import org.jetbrains.kotlin.idea.debugger.coroutines.view.XCoroutineThreadView

class CoroutineThreadViewDebugSessionListener(
    private val session: XDebugSession,
    private val xCoroutineThreadView: XCoroutineThreadView
) : XDebugSessionListener {
    val log by logger

    override fun sessionPaused() {
        val suspendContext = session.suspendContext ?: return requestClear()
        xCoroutineThreadView.forceClear()
        renew(suspendContext)
    }

    override fun sessionResumed() {
        val suspendContext = session.suspendContext ?: return requestClear()
        log.warn("sessionResumed ${session}")
        renew(suspendContext)
    }

    override fun sessionStopped() {
        val suspendContext = session.suspendContext ?: return requestClear()
        log.warn("sessionStopped ${session}")
        renew(suspendContext)
    }

    override fun stackFrameChanged() {
        val suspendContext = session.suspendContext ?: return requestClear()
        log.warn("stackFrameChanged ${session}")
        renew(suspendContext)
    }

    override fun beforeSessionResume() {
        log.warn("beforeSessionResume ${session}")
    }

    override fun settingsChanged() {
        val suspendContext = session.suspendContext ?: return requestClear()
        log.warn("settingsChanged ${session}")
        renew(suspendContext)
    }

    fun renew(suspendContext: XSuspendContext) {
        DebuggerUIUtil.invokeLater {
            xCoroutineThreadView.panel.tree.setRoot(xCoroutineThreadView.createRoot(suspendContext), false)
        }
    }

    fun requestClear() {
        if (ApplicationManager.getApplication().isUnitTestMode) { // no delay in tests
            xCoroutineThreadView.clear()
        } else {
            xCoroutineThreadView.alarm.cancelAndRequest()
        }
    }
}