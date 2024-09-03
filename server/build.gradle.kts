val mainName = "dev.voroscsoki.stratopolis.server.Main"
plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set(mainName) // Replace with your main class
}

dependencies {
    implementation(kotlin("stdlib"))
    // Add any other dependencies you need for your backend, e.g., Ktor, Exposed, etc.
}

tasks.register("buildBackend", GradleBuild::class) {
    dependsOn("build")
}

tasks.register("runBackend", JavaExec::class) {
    group = "application"
    mainClass.set(this@Build_gradle.mainName) // Replace with your main class
    classpath = sourceSets["main"].runtimeClasspath
    // Add any other necessary configurations
}

