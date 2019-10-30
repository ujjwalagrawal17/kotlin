import org.gradle.api.GradleException
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootExtension

plugins {
    kotlin("js") version "<pluginMarkerVersion>"
}

group = "com.example"
version = "1.0"

repositories {
    mavenLocal()
    jcenter()
}

plugins.withType<YarnPlugin> {
    (extensions[YarnRootExtension.YARN] as YarnRootExtension).apply {
        tasks {
            val yarnFolderRemove by registering {
                doLast {
                    installationDir.deleteRecursively()
                }
            }

            val yarnFolderCheck by registering {
                dependsOn(getByName("kotlinYarnSetup"))

                doLast {
                    if (!installationDir.exists()) {
                        throw GradleException()
                    }
                }
            }

            val yarnConcreteVersionFolderChecker by registering {
                dependsOn(getByName("kotlinYarnSetup"))

                doLast {
                    if (!installationDir.resolve("yarn-v1.9.3").exists()) {
                        throw GradleException()
                    }
                }
            }
        }
    }
}

kotlin.sourceSets {
    getByName("main") {
        dependencies {
            implementation(kotlin("stdlib-js"))
        }
    }
}

kotlin.target {
    nodejs()
}