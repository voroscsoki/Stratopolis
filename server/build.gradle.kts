val mainName = "dev.voroscsoki.stratopolis.server.Main"
plugins {
    kotlin("jvm")
    application
    id("io.ktor.plugin") version "2.3.12"
}

application {
    mainClass.set(mainName)
}

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

dependencies {
    implementation(kotlin("stdlib"))
    implementation("de.topobyte:osm4j-core:1.3.0")
    implementation("de.topobyte:osm4j-geometry:1.3.0")
    implementation("de.topobyte:osm4j-xml:1.3.0")
    implementation("de.topobyte:osm4j-pbf:1.3.0")
    implementation("de.topobyte:osm4j-pbf-full-runtime:1.3.0")
    implementation("de.topobyte:osm4j-tbo:1.3.0")
    implementation("de.topobyte:osm4j-utils:1.3.0")
    implementation("de.topobyte:osm4j-extra:1.3.0")
    implementation("de.topobyte:osm4j-incubating:1.3.0")
    implementation("de.topobyte:osm4j-replication:1.3.0")
    implementation("de.topobyte:osm4j-testing:1.3.0")

    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-config-yaml")
    implementation(project(":common"))
}

tasks.register("buildBackend", GradleBuild::class) {
    dependsOn("build")
}

tasks.register("runBackend", JavaExec::class) {
    group = "application"
    mainClass.set(this@Build_gradle.mainName)
    classpath = sourceSets["main"].compileClasspath
}

