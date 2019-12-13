/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.util.findLabelAndCall
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.inline.InlineUtil

class ChangeToLabeledReturnFix(
    element: KtReturnExpression, val labeledReturn: String
) : KotlinQuickFixAction<KtReturnExpression>(element) {

    override fun getFamilyName() = "Change to return with label"
    override fun getText() = "Change to '$labeledReturn'"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val returnExpression = element ?: return
        val factory = KtPsiFactory(project)
        val returnedExpression = returnExpression.returnedExpression
        val newExpression = if (returnedExpression == null)
            factory.createExpression(labeledReturn)
        else
            factory.createExpressionByPattern("$0 $1", labeledReturn, returnedExpression)
        returnExpression.replace(newExpression)
    }

    companion object : KotlinIntentionActionsFactory() {
        private fun findAccessibleLabels(bindingContext: BindingContext, position: KtReturnExpression): List<Name> {
            val result = mutableListOf<Name>()
            for (parent in position.parentsWithSelf) {
                if (parent is KtFunctionLiteral) {
                    val (label, call) = parent.findLabelAndCall()
                    if (label != null) {
                        result.add(label)
                    }

                    // check if the current function literal is inlined and stop processing outer declarations if it's not
                    val callee = call?.calleeExpression as? KtReferenceExpression ?: break
                    if (!InlineUtil.isInline(bindingContext[BindingContext.REFERENCE_TARGET, callee])) break
                }
            }
            return result
        }

        override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
            val expression = diagnostic.psiElement as? KtReturnExpression ?: return emptyList()
            return findAccessibleLabels(expression.analyze(), expression).map {
                ChangeToLabeledReturnFix(expression, labeledReturn = "return@${it.render()}")
            }
        }
    }
}