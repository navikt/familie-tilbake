plugins {
    id("java")
}

group = "no.nav"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    api(project(":modell"))
    api("io.kotest:kotest-assertions-core:6.2.6")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
