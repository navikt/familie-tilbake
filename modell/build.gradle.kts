plugins {
    kotlin("jvm")
}

group = "no.nav"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":kontrakter-intern"))
    api(project(":kontrakter-ekstern-v2"))

    testImplementation("org.junit.jupiter:junit-jupiter:5.12.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
}

tasks.test {
    useJUnitPlatform()
}
