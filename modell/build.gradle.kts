plugins {
    kotlin("plugin.serialization") version "1.9.23"
}
dependencies {
    api(project(":kontrakter-intern"))
    api(project(":kontrakter-ekstern-v2"))
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.3")
}
