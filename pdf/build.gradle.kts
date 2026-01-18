val openHtmlToPdfVersion = "1.1.36"
val veraPdfVersion = "1.28.2"

dependencies {
    api(project(":kontrakter-felles"))
    api(project(":kontrakter-intern"))
    api(project(":felles"))

    api("com.github.jknack:handlebars:4.5.0")
    api("com.github.jknack:handlebars-jackson2:4.3.1")

    api("io.github.openhtmltopdf:openhtmltopdf-core:$openHtmlToPdfVersion")
    api("io.github.openhtmltopdf:openhtmltopdf-pdfbox:$openHtmlToPdfVersion")
    api("io.github.openhtmltopdf:openhtmltopdf-slf4j:$openHtmlToPdfVersion")
    api("io.github.openhtmltopdf:openhtmltopdf-svg-support:$openHtmlToPdfVersion")
    api("org.verapdf:core-jakarta:$veraPdfVersion")
    api("org.verapdf:validation-model-jakarta:$veraPdfVersion")

    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
}
