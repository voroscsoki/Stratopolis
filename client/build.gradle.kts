val mainName = "dev.voroscsoki.stratopolis.client.Main"
val kotlinVersion = "2.3.12"
val gdxVersion = "1.13.0"
val ashleyVersion= "1.7.4"
plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version "2.0.20"
    id("io.ktor.plugin") version "2.3.12"
    application
}

application {
    mainClass = mainName
}

dependencies {
    implementation(kotlin("stdlib"))

    // LibGDX dependencies
    implementation("com.badlogicgames.ashley:ashley:$ashleyVersion")
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")

    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-okhttp-jvm")
    implementation("io.ktor:ktor-client-websockets")
    implementation("io.ktor:ktor-client-serialization")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
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

