import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    kotlin("jvm")
    id("org.openapi.generator") version "7.20.0" apply false
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
        remoteInputSpec = "https://raw.githubusercontent.com/navikt/tilbakekreving-kontrakter/d63d4e2b48225e863d4db7d93e7438edf77e3e5b/tsp-output/schema/openapi.yaml"
        outputDir = "$projectDir/src/main/generated"
        packageName = "no.nav.tilbakekreving.kontrakter.frontend"
        modelNameSuffix = "Dto"
        cleanupOutput = true
        configOptions.put("dateLibrary", "java8")
        configOptions.put("useSealed", "true")
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
        compileOnly("io.swagger.core.v3:swagger-annotations:2.2.43")
        compileOnly("jakarta.validation:jakarta.validation-api:3.1.1")
    }
}

repositories {
    mavenCentral()
}
