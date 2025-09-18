plugins {
    kotlin("jvm")
}

group = "no.nav"

repositories {
    mavenCentral()
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-annotations:2.20")
    api(project("::kontrakter-felles"))
}
