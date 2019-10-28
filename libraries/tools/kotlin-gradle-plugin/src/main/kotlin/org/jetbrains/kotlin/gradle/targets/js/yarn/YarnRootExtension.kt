/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.yarn

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.logging.kotlinInfo
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsPlatform
import org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin

open class YarnRootExtension(val project: Project) {
    init {
        check(project == project.rootProject)
    }

    private val gradleHome = project.gradle.gradleUserHomeDir.also {
        project.logger.kotlinInfo("Storing cached files in $it")
    }

    var installationDir = gradleHome.resolve("yarn")

    var download = true

    private var _command: String? = null
    var command: String
        get() = _command ?: "yarn.js"
        set(value) {
            _command = value
        }

    var downloadBaseUrl = "https://github.com/yarnpkg/yarn/releases/download"
    var version = "1.15.2"

    val yarnSetupTask: YarnSetupTask
        get() = project.tasks.getByName(YarnSetupTask.NAME) as YarnSetupTask

    var disableWorkspaces: Boolean = false

    val useWorkspaces: Boolean
        get() = !disableWorkspaces

    internal fun executeSetup() {
        NodeJsRootPlugin.apply(project).executeSetup()

        val env = environment
        if (download && !env.home.isDirectory) {
            yarnSetupTask.setup()
        }
    }

    internal val environment: YarnEnv
        get() {
            val home = installationDir.resolve("yarn-v$version")

            val isWindows = NodeJsPlatform.name == NodeJsPlatform.WIN

            return YarnEnv(
                downloadUrl = "$downloadBaseUrl/v$version/yarn-v$version.tar.gz",
                home = home,
                executable = if (!download) {
                    if (_command == null) "yarn${if (isWindows) ".cmd" else ""}" else command
                } else home.resolve("bin/$command").absolutePath
            )
        }

    companion object {
        const val YARN: String = "kotlinYarn"

        operator fun get(project: Project): YarnRootExtension {
            val rootProject = project.rootProject
            rootProject.plugins.apply(YarnPlugin::class.java)
            return rootProject.extensions.getByName(YARN) as YarnRootExtension
        }
    }
}

val Project.yarn: YarnRootExtension
    get() = YarnRootExtension[this]