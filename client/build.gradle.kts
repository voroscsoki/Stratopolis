val mainName = "dev.voroscsoki.stratopolis.client.Main"
plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.0.20"
    id("io.ktor.plugin") version "2.3.12"
    application
}

application {
    mainClass.set(mainName) // Replace with your frontend launcher class
}

dependencies {
    implementation(kotlin("stdlib"))

    // LibGDX dependencies
    implementation("com.badlogicgames.gdx:gdx:1.12.1")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:1.10.0")
    implementation("com.badlogicgames.gdx:gdx-platform:1.10.0:natives-desktop")

    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-okhttp-jvm:2.3.12")
    implementation("io.ktor:ktor-server-websockets")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.2")
    implementation(project(":common"))
}

tasks.register("buildFrontend", GradleBuild::class) {
    dependsOn("build")
}

tasks.register("runFrontend", JavaExec::class) {
    group = "application"
    mainClass.set(mainName)
    classpath = sourceSets["main"].runtimeClasspath
}

