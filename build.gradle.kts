plugins {
    kotlin("jvm") version "2.0.20" apply false
}

allprojects {
    repositories {
        mavenCentral()
        //add mvn.topobyte.de
        maven {
            url = uri("https://mvn.topobyte.de/")
        }
        maven {
            url = uri("https://mvn.slimjars.com/")
        }
        maven {
            url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap")
        }
        maven {
            url = uri("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
        }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

tasks.register("runAll") {
    dependsOn(":server:runBackend", ":client:runFrontend")
}