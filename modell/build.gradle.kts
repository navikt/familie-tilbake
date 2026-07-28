dependencies {
    api(project(":kontrakter-intern"))
    api(project(":kontrakter-ekstern-v2"))
    api(project(":kontrakter-frontend:dtoer"))
    testImplementation(project(":testdata"))
    api(project(":felles"))
    api("com.fasterxml.jackson.core:jackson-databind:2.22.1")
    implementation("io.ktor:ktor-http:3.5.1")
}
