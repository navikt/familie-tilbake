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
    testImplementation(platform("org.junit:junit-bom:6.1.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
