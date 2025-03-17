plugins {
    kotlin("jvm")
}

group = "no.nav"

repositories {
    mavenCentral()
}

dependencies {
    api("jakarta.validation:jakarta.validation-api:3.1.1")
    api("com.fasterxml.jackson.core:jackson-annotations:2.18.3")
    api(project("::kontrakter-felles"))
}

