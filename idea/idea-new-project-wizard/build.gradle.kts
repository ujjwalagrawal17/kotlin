plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    compile(project(":libraries:tools:new-project-wizard"))
    compile(project(":idea:ide-common"))
    compile(project(":idea:idea-core"))
    compile(project(":idea:idea-jvm"))

    compileOnly(intellijPluginDep("gradle"))
    compileOnly(intellijPluginDep("android"))

    excludeInAndroidStudio(rootProject) {
        compileOnly(intellijPluginDep("maven"))
    }

    compileOnly(project(":kotlin-reflect-api"))

    compileOnly(intellijCoreDep()) {
        includeJars(
            "intellij-core",
            "guava",
            rootProject = rootProject
        )
    }
    compileOnly(intellijDep()) {
        includeJars(
            "intellij-dvcs",
            "platform-api",
            "platform-impl",
            "platform-core-ui",
            "platform-util-ui",
            rootProject = rootProject
        )
    }

    Platform[191].orLower {
        compileOnly(intellijDep()) { includeJars("java-api", "java-impl") }
    }

    Platform[192].orHigher {
        compileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl") }
        testCompileOnly(intellijPluginDep("java")) { includeJars("java-api", "java-impl") }
    }
}


sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    dependsOn(":dist")
    workingDir = rootDir
}
