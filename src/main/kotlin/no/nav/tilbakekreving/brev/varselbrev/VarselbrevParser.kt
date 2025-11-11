package no.nav.tilbakekreving.brev.varselbrev

data class Varselbrevtekst(
    val overskrift: String,
    val avsnitter: List<Section>,
)

data class Section(val title: String, val body: String)

object VarselbrevParser {
    private val headerRegex = Regex("""^\s*_(.+)\s*$""")
    private val dropLineRegex = Regex("""^\s*Del\s*\d+\s*av\s*tekster\s*$""", RegexOption.IGNORE_CASE)
    private val signatureStartRegex = Regex("""(?is)\bMed\s+vennlig\s+hilsen\b.*$""")

    fun parse(raw: String): MutableList<Section> {
        val text = raw
            .replace("\r\n", "\n")
            .replace('\u2028', '\n')
            .replace(signatureStartRegex, "")
            .trim()

        val sections = mutableListOf<Section>()
        var currentTitle: String? = ""
        val body = StringBuilder()

        fun flush() {
            currentTitle?.let {
                sections += Section(it.trim(), normalizeBody(body.toString()))
            }
            currentTitle = null
            body.setLength(0)
        }

        text.lines().forEach { line ->
            if (dropLineRegex.matches(line)) return@forEach
            val m = headerRegex.find(line)
            if (m != null) {
                flush()
                currentTitle = m.groupValues[1].trim()
            } else if (currentTitle != null) {
                body.appendLine(line)
            }
        }
        flush()
        return sections
    }

    private fun normalizeBody(body: String): String {
        val cleaned = body.lines()
            .map { it.trimEnd() }
            .dropWhile { it.isBlank() }
            .dropLastWhile { it.isBlank() }

        val out = StringBuilder()
        var lastBlank = false
        for (l in cleaned) {
            val blank = l.isBlank()
            if (blank && lastBlank) continue
            out.appendLine(l)
            lastBlank = blank
        }
        return out.toString().trimEnd()
    }
}
