/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutines.util

import javax.swing.Icon
import javax.swing.JComponent

interface ContentParamProvider {
    fun provideContentParam() : CreateContentParam
}

data class CreateContentParam(val id: String, val component: JComponent, val displayName: String, val icon: Icon?, val parentComponent: JComponent)