dependencies {
    api(project(":kontrakter-intern"))
    api(project(":kontrakter-ekstern-v2"))
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.ktor:ktor-http:3.2.3")
}
