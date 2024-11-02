val mainName = "dev.voroscsoki.stratopolis.common.Main"
plugins {
    application
    kotlin("plugin.serialization") version "2.0.20"
    id("io.ktor.plugin") version "2.3.12"
}

application {
    mainClass = mainName
}

val osm4jVersion = "1.3.0"
dependencies {
    implementation(kotlin("stdlib"))
    implementation("de.topobyte:osm4j-core:$osm4jVersion")
    implementation("de.topobyte:osm4j-geometry:$osm4jVersion")
    implementation("de.topobyte:osm4j-xml:$osm4jVersion")
    implementation("de.topobyte:osm4j-pbf:$osm4jVersion")
    implementation("de.topobyte:osm4j-pbf-full-runtime:$osm4jVersion")
    implementation("de.topobyte:osm4j-tbo:$osm4jVersion")
    implementation("de.topobyte:osm4j-utils:$osm4jVersion")
    implementation("de.topobyte:osm4j-extra:$osm4jVersion")
    implementation("de.topobyte:osm4j-incubating:$osm4jVersion")
    implementation("de.topobyte:osm4j-replication:$osm4jVersion")
    implementation("de.topobyte:osm4j-testing:$osm4jVersion")

    implementation("io.ktor:ktor-client-websockets")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
}
