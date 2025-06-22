plugins {
    kotlin("jvm")
}

group = "no.nav"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    api("jakarta.validation:jakarta.validation-api:3.1.1")
    api("com.fasterxml.jackson.core:jackson-annotations:2.19.1")
    api(project("::kontrakter-felles"))
}
