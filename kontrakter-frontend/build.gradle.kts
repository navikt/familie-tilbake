import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    kotlin("jvm")
    id("org.openapi.generator") version "7.18.0" apply false
}

group = "no.nav"

subprojects {
    plugins.apply("org.jetbrains.kotlin.jvm")
    plugins.apply("org.openapi.generator")

    sourceSets {
        main {
            kotlin.srcDir("src/main/generated")
        }
    }

    tasks.withType<KotlinCompile> {
        dependsOn("openApiGenerate")
    }

    tasks.withType<GenerateTask> {
        generatorName = "kotlin-spring"
        remoteInputSpec = "https://raw.githubusercontent.com/navikt/tilbakekreving-kontrakter/7e37eacd5fc373054f1e62bdccc5e6f24ae3ab85/tsp-output/schema/openapi.yaml"
        outputDir = "$projectDir/src/main/generated"
        packageName = "no.nav.kontrakter.frontend"
        modelNameSuffix = "Dto"
        cleanupOutput = true
        configOptions.put("dateLibrary", "java8")
        configOptions.put("interfaceOnly", "true")
        configOptions.put("useSpringBoot3", "true")
        configOptions.put("useTags", "true")
        configOptions.put("sourceFolder", "/")
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
        compileOnly("io.swagger.core.v3:swagger-annotations:2.2.40")
        compileOnly("jakarta.validation:jakarta.validation-api:3.1.1")
    }
}

repositories {
    mavenCentral()
}
