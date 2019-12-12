plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api("org.apache.velocity:velocity:1.7") // we have to use the old version as it is the same as bundled into IntelliJ
    compileOnly(project(":kotlin-reflect-api"))

    testCompile(project(":kotlin-test:kotlin-test-junit"))
    testCompile(commonDep("junit:junit"))
    testCompile(commonDep("junit:junit"))
    testCompile(commonDep("junit:junit"))
}


sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

projectTest {
    dependsOn(":dist")
    workingDir = rootDir
}

testsJar()
