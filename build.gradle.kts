import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.kotlin.dsl.register
import org.springframework.boot.gradle.plugin.SpringBootPlugin
import java.net.URI

val springDocVersion = "3.0.1"
val testcontainersVersion = "1.21.4"
val tokenValidationVersion = "6.0.2"
val flywayVersion = "11.3.4"
val ktorVersion = "3.4.0"
ext["ktorVersion"] = ktorVersion

group = "no.nav"
description = "familie-tilbake"
java.sourceCompatibility = JavaVersion.VERSION_21

plugins {
    kotlin("jvm") version "2.3.10"
    id("org.springframework.boot") version "4.0.2"
    id("org.jetbrains.kotlin.plugin.spring") version "2.3.10"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
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
        testImplementation("org.junit.jupiter:junit-jupiter:6.0.2")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
        testImplementation("io.kotest:kotest-assertions-core:6.1.3")
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

abstract class DownloadFileTask : DefaultTask() {
    @get:Input
    abstract val url: Property<String>

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun run() {
        val out = outputFile.get().asFile
        out.parentFile.mkdirs()

        URI.create(url.get()).toURL().openStream().use { input ->
            out.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}

val bigqueryDriverUrl =
    "https://storage.googleapis.com/simba-bq-release/jdbc/SimbaJDBCDriverforGoogleBigQuery42_1.6.5.1002.zip"

val bigQueryDriverDir = layout.buildDirectory.dir("libs/bigquery-jdbc")
val bigQueryZip = layout.buildDirectory.file("downloads/bigquery-jdbc.zip")

val downloadBigQueryDriver = tasks.register<DownloadFileTask>("downloadBigQueryJdbc") {
    url.set(bigqueryDriverUrl)
    outputFile.set(bigQueryZip)
}

val unzipBigQueryDriver = tasks.register<Copy>("unzipBigQueryDriver") {
    dependsOn(downloadBigQueryDriver)
    from(zipTree(bigQueryZip))
    into(bigQueryDriverDir)
}

val cleanupBigQueryDownloads = tasks.register<Delete>("cleanupBigQueryDownloads") {
    delete(layout.buildDirectory.dir("downloads"))
}

unzipBigQueryDriver.configure {
    finalizedBy(cleanupBigQueryDownloads)
}

val bigQueryDriver by configurations.creating

val bigQueryDriverJarsTree = fileTree(bigQueryDriverDir) {
    include("**/*.jar")

    exclude("**/httpclient5-*.jar")
    exclude("**/httpcore5-*.jar")
    exclude("**/httpcore5-h2-*.jar")
    exclude("**/httpclient-*.jar")
    exclude("**/httpcore-*.jar")

    exclude("**/jspecify-*.jar")
    exclude("**/opentelemetry-*.jar")
    exclude("**/commons-codec-*.jar")
    exclude("**/json-*.jar")
    exclude("**/protobuf-java-util-*.jar")
    exclude("**/threetenbp-*.jar")
    exclude("**/google-api-client-*.jar")
    exclude("**/google-oauth-client-*.jar")
    exclude("**/opencensus-api-*.jar")
    exclude("**/opencensus-contrib-http-util-*.jar")
    exclude("**/checker-compat-qual-*.jar")
    exclude("**/jsr305-*.jar")
    exclude("**/javax.annotation-api-*.jar")
    exclude("**/listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar")
    exclude("**/threeten-extra-*.jar")
    exclude("**/protobuf-java-*.jar")
    exclude("**/animal-sniffer-annotations-*.jar")
    exclude("**/annotations-*.jar")
    exclude("**/arrow-*.jar")
    exclude("**/conscrypt-openjdk-uber-*.jar")
    exclude("**/flatbuffers-java-*.jar")
    exclude("**/perfmark-api-*.jar")
    exclude("**/jackson-annotations-*.jar")
    exclude("**/jackson-core-*.jar")
    exclude("**/jackson-databind-*.jar")
    exclude("**/jackson-module-*.jar")
    exclude("**/jackson-datatype-*.jar")
}

val bigQueryDriverJars: FileCollection =
    files(bigQueryDriverJarsTree).builtBy(unzipBigQueryDriver)

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson-jvm:$ktorVersion")
    implementation("com.google.cloud:google-cloud-bigquery:2.59.0")
    implementation("org.apache.httpcomponents.client5:httpclient5")
    implementation("org.apache.httpcomponents.core5:httpcore5")
    implementation("org.apache.httpcomponents.core5:httpcore5-h2")

    api("org.springdoc:springdoc-openapi-starter-webmvc-ui:$springDocVersion")
    api("org.springdoc:springdoc-openapi-starter-common:$springDocVersion")
    api("org.springframework.boot:spring-boot-starter-actuator")
    api("org.springframework.boot:spring-boot-starter-jetty")
    api("org.springframework.boot:spring-boot-starter-validation")
    api("org.springframework.boot:spring-boot-starter-web")
    api("org.springframework.retry:spring-retry")

    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")

    api("com.ibm.mq:com.ibm.mq.jakarta.client:9.4.5.0")
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
    api(project(":kontrakter-frontend:api"))
    api(project(":modell"))
    api(project(":pdf"))
    api("no.nav.familie:prosessering-core:2.20250728105838_1f618e2") {
        // La spring boot håndtere flyway versjon selv om den er eldre enn den som er inkludert i prosessering-core
        exclude("org.flywaydb")
    }
    api("no.nav.familie.tjenestespesifikasjoner:tilbakekreving-v1-tjenestespesifikasjon:1.0_20250425112447_49835df")
    api("no.nav.tjenestespesifikasjoner:avstemming-v1-tjenestespesifikasjon:2648.7dd4e44")

    api("no.nav.security:token-client-core:$tokenValidationVersion")
    api("no.nav.security:token-client-spring:$tokenValidationVersion")
    api("no.nav.security:token-validation-core:$tokenValidationVersion")
    api("no.nav.security:token-validation-spring:$tokenValidationVersion")

    api("ch.qos.logback:logback-classic")
    api("com.papertrailapp:logback-syslog4j:1.0.0")
    api("io.micrometer:micrometer-registry-prometheus")
    api("net.logstash.logback:logstash-logback-encoder:9.0")

    api("io.getunleash:unleash-client-java:12.1.1")
    api("org.messaginghub:pooled-jms:3.2.2")
    api("org.flywaydb:flyway-core")
    api("org.flywaydb:flyway-gcp-bigquery")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly(bigQueryDriverJars)

    testImplementation("org.junit.jupiter:junit-jupiter:6.0.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("io.mockk:mockk-jvm:1.14.9")
    testImplementation("io.kotest:kotest-assertions-core-jvm:6.1.3")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    testImplementation("io.jsonwebtoken:jjwt:0.13.0")
    testImplementation("no.nav.security:token-validation-spring-test:$tokenValidationVersion")

    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:activemq:$testcontainersVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")

    testImplementation("org.apache.kafka:kafka_2.13")
    testImplementation("org.wiremock:wiremock-standalone:3.13.2")
    testImplementation("org.apache.activemq:activemq-client")
}
