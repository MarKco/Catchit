plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

application {
    mainClass = "com.ilsecondodasinistra.catchit.importer.MainKt"
}

dependencies {
    implementation(project(":timetable-builder"))
}

tasks.named<JavaExec>("run") {
    workingDir = rootDir
    // The download can take a while: stream the logs.
    standardOutput = System.out
}
