import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    kotlin("jvm")
    id("org.openapi.generator") version "7.22.0" apply false
}

group = "no.nav"

subprojects {
    plugins.apply("org.jetbrains.kotlin.jvm")
    plugins.apply("org.openapi.generator")

    sourceSets {
        main {
            java.srcDir("src/main/generated")
        }
    }

    tasks.withType<KotlinCompile> {
        dependsOn("openApiGenerate")
    }

    tasks.withType<GenerateTask> {
        generatorName = "kotlin-spring"
        remoteInputSpec = "https://raw.githubusercontent.com/navikt/tilbakekreving-kontrakter/74c59469a62c2071bdac8777b8821b8c11efee3c/tsp-output/schema/openapi.yaml"
        outputDir = projectDir.resolve("src/main/generated")
        packageName = "no.nav.tilbakekreving.kontrakter.frontend"
        modelNameSuffix = "Dto"
        cleanupOutput = true
        configOptions.put("dateLibrary", "java8")
        configOptions.put("useSealed", "true")
        configOptions.put("interfaceOnly", "true")
        configOptions.put("useSpringBoot3", "true")
        configOptions.put("useBeanValidation", "false")
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
        compileOnly("io.swagger.core.v3:swagger-annotations:2.2.49")
        compileOnly("jakarta.validation:jakarta.validation-api:3.1.1")
    }
}

repositories {
    mavenCentral()
}
