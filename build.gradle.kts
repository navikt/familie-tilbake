import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.springframework.boot.gradle.plugin.SpringBootPlugin

val openHtmlToPdfVersion = "1.1.24"
val springDocVersion = "2.8.5"
val testcontainersVersion = "1.20.5"
val tokenValidationVersion = "5.0.17"
val veraPdfVersion = "1.26.5"
val flywayVersion = "11.3.4"

group = "no.nav"
description = "familie-tilbake"
java.sourceCompatibility = JavaVersion.VERSION_21

plugins {
    kotlin("jvm") version "2.1.10"
    id("org.springframework.boot") version "3.4.3"
    id("org.jetbrains.kotlin.plugin.spring") version "2.1.10"
    id("org.jlleitschuh.gradle.ktlint") version "12.2.0"
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

repositories {
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
    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
    api("org.springdoc:springdoc-openapi-starter-common:$springDocVersion")
    api("org.springframework.boot:spring-boot-starter-actuator")
    api("org.springframework.boot:spring-boot-starter-jetty")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.retry:spring-retry")

    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")

    api("com.github.jknack:handlebars:4.4.0")
    api("com.github.jknack:handlebars-jackson2:4.3.1")

    api("io.github.openhtmltopdf:openhtmltopdf-core:$openHtmlToPdfVersion")
    api("io.github.openhtmltopdf:openhtmltopdf-pdfbox:$openHtmlToPdfVersion")
    api("io.github.openhtmltopdf:openhtmltopdf-slf4j:$openHtmlToPdfVersion")
    api("io.github.openhtmltopdf:openhtmltopdf-svg-support:$openHtmlToPdfVersion")
    api("org.verapdf:core-jakarta:$veraPdfVersion")
    api("org.verapdf:validation-model-jakarta:$veraPdfVersion")

    api("com.ibm.mq:com.ibm.mq.jakarta.client:9.4.1.1")
    api("jakarta.jms:jakarta.jms-api")
    api("org.apache.activemq:activemq-jms-pool")
    api("org.springframework:spring-jms")

    api("org.postgresql:postgresql")
    api("org.springframework.boot:spring-boot-starter-data-jdbc")

    api("org.apache.kafka:kafka-clients")
    api("org.springframework.kafka:spring-kafka")

    api("no.nav.familie:prosessering-core:2.20250219093533_62416e5") {
        // La spring boot håndtere flyway versjon selv om den er eldre enn den som er inkludert i prosessering-core
        exclude("org.flywaydb")
    }
    api("no.nav.familie.tjenestespesifikasjoner:tilbakekreving-v1-tjenestespesifikasjon:1.0_20250103091213_2eaa779")
    api("no.nav.tjenestespesifikasjoner:avstemming-v1-tjenestespesifikasjon:2641.575768a")

    api("no.nav.security:token-client-core:$tokenValidationVersion")
    api("no.nav.security:token-client-spring:$tokenValidationVersion")
    api("no.nav.security:token-validation-core:$tokenValidationVersion")
    api("no.nav.security:token-validation-spring:$tokenValidationVersion")

    api("ch.qos.logback:logback-classic")
    api("com.papertrailapp:logback-syslog4j:1.0.0")
    api("io.micrometer:micrometer-registry-prometheus")
    api("net.logstash.logback:logstash-logback-encoder:8.0")

    api("io.getunleash:unleash-client-java:10.0.1")
    api("org.messaginghub:pooled-jms:3.1.7")
    api("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")

    testImplementation("org.junit.jupiter:junit-jupiter:5.12.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("io.mockk:mockk-jvm:1.13.16")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.9.1")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock:4.2.0")
    testImplementation("org.springframework.kafka:spring-kafka-test")

    testImplementation("io.jsonwebtoken:jjwt:0.12.6")
    testImplementation("no.nav.security:token-validation-spring-test:$tokenValidationVersion")

    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")

    testImplementation("org.apache.kafka:kafka_2.13")
    testImplementation("org.wiremock:wiremock-standalone:3.12.0")
}
