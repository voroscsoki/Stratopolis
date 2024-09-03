val mainName = "dev.voroscsoki.stratopolis.client.Main"
plugins {
    kotlin("jvm")
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
}

tasks.register("buildFrontend", GradleBuild::class) {
    dependsOn("build")
}

tasks.register("runFrontend", JavaExec::class) {
    group = "application"
    mainClass.set(mainName) // Replace with your main class
    classpath = sourceSets["main"].runtimeClasspath
    // Add any other necessary configurations
}

