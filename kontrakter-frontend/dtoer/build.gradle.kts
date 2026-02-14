openApiGenerate {
    ignoreFileOverride = "$projectDir/.openapi-generator-ignore"
    globalProperties.put("models", "")
}

dependencies {
    compileOnly("com.fasterxml.jackson.core:jackson-annotations:2.20")
}
