val mainName = "dev.voroscsoki.stratopolis.common.Main"
plugins {
    application
    kotlin("plugin.serialization") version "2.0.20"
    id("io.ktor.plugin") version "2.3.12"
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

    implementation("io.ktor:ktor-client-websockets")
    implementation("io.ktor:ktor-server-core")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")
}
