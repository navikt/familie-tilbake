plugins {
    kotlin("jvm")
}

group = "no.nav"

repositories {
    mavenCentral()
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-annotations:2.19.1")
    api(project("::kontrakter-felles"))
}
