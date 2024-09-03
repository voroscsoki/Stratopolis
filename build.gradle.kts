plugins {
    kotlin("jvm") version "2.0.20" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

tasks.register("buildAll") {
    dependsOn(":server:buildBackend", ":client:buildFrontend")
}

tasks.register("runAll") {
    dependsOn(":server:runBackend", ":client:runFrontend")
}

tasks.register("buildAndRunAll") {
    dependsOn("buildAll")
    dependsOn("runAll")
}