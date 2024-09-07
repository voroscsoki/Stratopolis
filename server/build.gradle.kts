val mainName = "dev.voroscsoki.stratopolis.server.Main"
plugins {
    kotlin("jvm")
    application
    id("io.ktor.plugin") version "2.3.12"
}

application {
    mainClass.set(mainName)
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
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
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

