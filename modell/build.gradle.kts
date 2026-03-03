dependencies {
    api(project(":kontrakter-intern"))
    api(project(":kontrakter-ekstern-v2"))
    api(project(":kontrakter-frontend:dtoer"))
    api(project(":testdata"))
    api(project(":felles"))
    api("com.fasterxml.jackson.core:jackson-databind:2.21.0")
    implementation("io.ktor:ktor-http:3.4.0")
}
