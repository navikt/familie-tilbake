import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.springframework.boot.gradle.plugin.SpringBootPlugin

val springDocVersion = "2.8.9"
val testcontainersVersion = "1.21.3"
val tokenValidationVersion = "5.0.30"
val flywayVersion = "11.3.4"
val ktorVersion = "3.2.1"
ext["ktorVersion"] = ktorVersion

group = "no.nav"
description = "familie-tilbake"
java.sourceCompatibility = JavaVersion.VERSION_21

plugins {
    kotlin("jvm") version "2.2.0"
    id("org.springframework.boot") version "3.5.3"
    id("org.jetbrains.kotlin.plugin.spring") version "2.2.0"
    id("org.jlleitschuh.gradle.ktlint") version "13.0.0"
}

springBoot {
    mainClass = "no.nav.familie.tilbake.LauncherKt"
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging.showStandardStreams = true
    testLogging {
        events("passed", "skipped", "failed")
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
    }
}

kotlin {
    jvmToolchain(21)
}

configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
    outputToConsole.set(true)
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "org.jetbrains.kotlin.jvm")

    group = "no.nav"

    repositories {
        mavenCentral()
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter:5.13.3")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
        testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    }

    tasks.test {
        useJUnitPlatform()
    }
}

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }

    maven {
        url = uri("https://maven.pkg.github.com/navikt/familie-tjenestespesifikasjoner")
        credentials {
            username = "x-access-token"
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
    api("org.springdoc:springdoc-openapi-starter-common:$springDocVersion")
    api("org.springframework.boot:spring-boot-starter-actuator")
    api("org.springframework.boot:spring-boot-starter-jetty")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.retry:spring-retry")

    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")

    api("com.ibm.mq:com.ibm.mq.jakarta.client:9.4.3.0")
    api("jakarta.jms:jakarta.jms-api")
    api("org.apache.activemq:activemq-jms-pool")
    api("org.springframework:spring-jms")

    api("org.postgresql:postgresql")
    api("org.springframework.boot:spring-boot-starter-data-jdbc")

    api("org.apache.kafka:kafka-clients")
    api("org.springframework.kafka:spring-kafka")

    api(project(":felles"))
    api(project(":integrasjoner"))
    api(project(":kontrakter-ekstern"))
    api(project(":kontrakter-intern"))
    api(project(":modell"))
    api(project(":pdf"))
    api("no.nav.familie:prosessering-core:2.20250630085333_3794bb5") {
        // La spring boot håndtere flyway versjon selv om den er eldre enn den som er inkludert i prosessering-core
        exclude("org.flywaydb")
    }
    api("no.nav.familie.tjenestespesifikasjoner:tilbakekreving-v1-tjenestespesifikasjon:1.0_20250425112447_49835df")
    api("no.nav.tjenestespesifikasjoner:avstemming-v1-tjenestespesifikasjon:2643.2f3e8e9")

    api("no.nav.security:token-client-core:$tokenValidationVersion")
    api("no.nav.security:token-client-spring:$tokenValidationVersion")
    api("no.nav.security:token-validation-core:$tokenValidationVersion")
    api("no.nav.security:token-validation-spring:$tokenValidationVersion")

    api("ch.qos.logback:logback-classic")
    api("com.papertrailapp:logback-syslog4j:1.0.0")
    api("io.micrometer:micrometer-registry-prometheus")
    api("net.logstash.logback:logstash-logback-encoder:8.1")

    api("io.getunleash:unleash-client-java:11.0.2")
    api("org.messaginghub:pooled-jms:3.1.7")
    api("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    testImplementation("org.junit.jupiter:junit-jupiter:5.13.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("io.mockk:mockk-jvm:1.14.4")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.9.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock:4.3.0")
    testImplementation("org.springframework.kafka:spring-kafka-test")

    testImplementation("io.jsonwebtoken:jjwt:0.12.6")
    testImplementation("no.nav.security:token-validation-spring-test:$tokenValidationVersion")

    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:activemq:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")

    testImplementation("org.apache.kafka:kafka_2.13")
    testImplementation("org.wiremock:wiremock-standalone:3.13.1")
    testImplementation("org.apache.activemq:activemq-client")
}
