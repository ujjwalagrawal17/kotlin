/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.scripting.gradle.importing

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ProjectData
import com.intellij.openapi.util.Pair
import com.intellij.util.Consumer
import org.gradle.tooling.model.idea.IdeaProject
import org.gradle.tooling.model.kotlin.dsl.EditorReportSeverity
import org.gradle.tooling.model.kotlin.dsl.KotlinDslModelsParameters.*
import org.gradle.tooling.model.kotlin.dsl.KotlinDslScriptsModel
import org.jetbrains.kotlin.idea.scripting.gradle.kotlinDslScriptsModelImportSupported
import org.jetbrains.kotlin.idea.scripting.gradle.minimal_gradle_version_supported
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension

abstract class KotlinDslScriptModelResolverCommon : AbstractProjectResolverExtension() {
    override fun getExtraProjectModelClasses(): Set<Class<out Any>> {
        return setOf(KotlinDslScriptsModel::class.java)
    }

    override fun getExtraJvmArgs(): List<Pair<String, String>> {
        return listOf(
            Pair(
                "-D$PROVIDER_MODE_SYSTEM_PROPERTY_NAME",
                STRICT_CLASSPATH_MODE_SYSTEM_PROPERTY_VALUE
            )
        )
    }

    override fun enhanceTaskProcessing(taskNames: MutableList<String>, jvmParametersSetup: String?, initScriptConsumer: Consumer<String>) {
        initScriptConsumer.consume(
            "if (org.gradle.util.GradleVersion.current() >= org.gradle.util.GradleVersion.version(\"$minimal_gradle_version_supported\")) startParameter.taskNames += [\"${PREPARATION_TASK_NAME}\"]"
        )
    }

    override fun populateProjectExtraModels(gradleProject: IdeaProject, ideProject: DataNode<ProjectData>) {
        super.populateProjectExtraModels(gradleProject, ideProject)

        if (kotlinDslScriptsModelImportSupported(resolverCtx.projectGradleVersion)) {
            pupulateBuildModels(gradleProject, ideProject)

            resolverCtx.models.includedBuilds.forEach { includedRoot ->
                pupulateBuildModels(includedRoot, ideProject)
            }
        }
    }

    private fun pupulateBuildModels(
        root: IdeaProject,
        ideProject: DataNode<ProjectData>
    ) {
        root.modules.forEach {
            if (it.gradleProject.parent == null) {
                resolverCtx.getExtraProject(it, KotlinDslScriptsModel::class.java)?.let { model ->
                    ideProject.KOTLIN_DSL_SCRIPT_MODELS.addAll(model.toListOfScriptModels())
                }
            }
        }
    }

    override fun requiresTaskRunning() = true

    @Suppress("unused")
    private fun KotlinDslScriptsModel.toListOfScriptModels(): List<KotlinDslScriptModel> =
        scriptModels.map { (file, model) ->
            val messages = mutableListOf<KotlinDslScriptModel.Message>()

            model.exceptions.forEach {
                messages.add(
                    KotlinDslScriptModel.Message(
                        KotlinDslScriptModel.Severity.ERROR, it
                    )
                )
            }

            model.editorReports.forEach {
                messages.add(
                    KotlinDslScriptModel.Message(
                        when (it.severity) {
                            EditorReportSeverity.WARNING -> KotlinDslScriptModel.Severity.WARNING
                            else -> KotlinDslScriptModel.Severity.ERROR
                        },
                        it.message,
                        it.position?.let { position ->
                            KotlinDslScriptModel
                                .Position(position.line, position.column)
                        }
                    ))
            }

            KotlinDslScriptModel(
                file.absolutePath,
                model.classPath.map { it.absolutePath },
                model.sourcePath.map { it.absolutePath },
                model.implicitImports,
                messages
            )
        }
}