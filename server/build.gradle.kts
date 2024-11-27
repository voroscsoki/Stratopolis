val mainName = "dev.voroscsoki.stratopolis.server.Main"
plugins {
    kotlin("jvm")
    application
    id("io.ktor.plugin") version "2.3.12"
    kotlin("plugin.serialization") version "2.0.20"
}

application {
    mainClass.set(mainName)
}

dependencies {
    val exposedVersion: String by project
    val osm4jversion = "1.3.0"

    implementation(kotlin("stdlib"))
    implementation("de.topobyte:osm4j-core:$osm4jversion")
    implementation("de.topobyte:osm4j-geometry:$osm4jversion")
    implementation("de.topobyte:osm4j-xml:$osm4jversion")
    implementation("de.topobyte:osm4j-pbf:$osm4jversion")
    implementation("de.topobyte:osm4j-pbf-full-runtime:$osm4jversion")
    implementation("de.topobyte:osm4j-tbo:$osm4jversion")
    implementation("de.topobyte:osm4j-utils:$osm4jversion")
    implementation("de.topobyte:osm4j-extra:$osm4jversion")
    implementation("de.topobyte:osm4j-incubating:$osm4jversion")
    implementation("de.topobyte:osm4j-replication:$osm4jversion")
    implementation("de.topobyte:osm4j-testing:$osm4jversion")

    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-netty")
    implementation("io.ktor:ktor-server-config-yaml")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-server-websockets")
    implementation("io.ktor:ktor-serialization-kotlinx-json")

    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-crypt:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")

    implementation("org.xerial:sqlite-jdbc:3.44.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")

    implementation(project(":common"))
}

tasks.register("buildBackend", GradleBuild::class) {
    dependsOn("build")
}

tasks.register("runBackend", JavaExec::class) {
    group = "application"
    mainClass.set(this@Build_gradle.mainName)
    classpath = sourceSets["main"].runtimeClasspath
}

