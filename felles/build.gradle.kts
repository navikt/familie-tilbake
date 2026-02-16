plugins {
    kotlin("jvm")
}

group = "no.nav"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    // TODO: Fjerne denne avhengigheten, datoperiode fra kontrakter og felles burde være ulik
    api(project(":kontrakter-intern"))
}

tasks.test {
    useJUnitPlatform()
}
