import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask

plugins {
    kotlin("jvm")
    id("org.openapi.generator") version "7.17.0"
}

group = "no.nav"

openApiGenerate {
    generatorName = "kotlin-spring"
    remoteInputSpec = "https://raw.githubusercontent.com/navikt/tilbakekreving-kontrakter/refs/heads/main/tsp-output/schema/openapi.yaml"
    outputDir = "$projectDir/src/main/generated"
    packageName = "no.nav.kontrakter.frontend"
    cleanupOutput = true
    configOptions.put("dateLibrary", "java8")
    configOptions.put("interfaceOnly", "true")
    configOptions.put("useSpringBoot3", "true")
    configOptions.put("sourceFolder", "/")
    globalProperties.put("models", "")
    globalProperties.put("apis", "")
}

sourceSets {
    main {
        kotlin.srcDir("src/main/generated")
    }
}

tasks.withType<KotlinCompile> {
    dependsOn("openApiGenerate")
}

tasks.withType<KtLintCheckTask> {
    dependsOn("openApiGenerate")
}

ktlint {
    ignoreFailures = true
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.springframework.boot:spring-boot-starter-web:4.0.0")
    api("io.swagger.core.v3:swagger-annotations:2.2.40")
    api("jakarta.validation:jakarta.validation-api:3.1.1")
}
