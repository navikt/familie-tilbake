import groovy.json.JsonOutput
import java.net.URI

plugins {
    java
}

val genererPostnummer = tasks.register("genererPostnummer") {
    val url = "https://www.bring.no/postnummerregister-ansi.txt"
    val outputDir = layout.buildDirectory.dir("generated/postnummer")

    group = "generate"
    description = "Genererer map fra postnummer til poststed"
    outputs.dir(outputDir)

    doLast {
        val postnummerMap = URI(url).toURL().openStream().bufferedReader(Charsets.ISO_8859_1).use { it.readLines() }
            .filter { it.isNotBlank() }
            .map { it.split('\t') }
            .associate { it[0].trim() to it[1].trim() }
            .toSortedMap()
        check(postnummerMap.isNotEmpty()) { "Fant ingen postnummer fra $url" }
        outputDir.get().asFile.mkdirs()
        outputDir.get().file("postnummer.json").asFile
            .writeText(JsonOutput.prettyPrint(JsonOutput.toJson(postnummerMap)), Charsets.UTF_8)
    }
}

sourceSets.main {
    resources.srcDir(genererPostnummer)
}
