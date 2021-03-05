package no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf

object DokprodTilHtml {

    fun dokprodInnholdTilHtml(dokprod: String): String {
        val builder = StringBuilder()
        val avsnittene = hentAvsnittene(dokprod)
        var samepageStarted = false
        avsnittene.forEach { avsnitt ->
            var inBulletpoints = false
            var harAvsnitt = false
            val linjer = avsnitt.split("\n\r?".toRegex())
            for (linje in linjer) {
                if (linje.isBlank()) {
                    builder.append("<br/>")
                    continue
                }
                if (linje.startsWith("*-")) {
                    inBulletpoints = true
                    builder.append("<ul>")
                    if (linje.substring(2).isBlank()) {
                        continue
                    }
                }
                if (inBulletpoints) {
                    val bp = linje.replace("*-", "")
                    if (bp.trimEnd().endsWith("-*")) {
                        builder.append("<li>").append(bp.replace("-*", "")).append("</li></ul>")
                        inBulletpoints = false
                    } else {
                        builder.append("<li>").append(bp).append("</li>")
                    }
                    continue
                }
                val overskrift = linje.startsWith("_")
                if (overskrift) {
                    if (samepageStarted) {
                        builder.append("</div>")
                    } else {
                        samepageStarted = true
                    }
                    builder.append("<div class=\"samepage\">")
                    builder.append("<h2>").append(linje.substring(1)).append("</h2>")
                } else {
                    if (!harAvsnitt) {
                        harAvsnitt = true
                        builder.append("<p>")
                    } else {
                        builder.append("<br/>")
                    }
                    builder.append(linje)
                }
            }
            if (harAvsnitt) {
                builder.append("</p>")
            }
            if (samepageStarted) {
                samepageStarted = false
                builder.append("</div>")
            }
        }
        return ekstraLinjeskiftFørHilsing(konverterNbsp(builder.toString()))
    }

    private fun hentAvsnittene(dokprod: String): List<String> {
        //avsnitt ved dobbelt linjeskift
        //avsnitt ved overskrift (linje starter med _)
        return dokprod.split("(\n\r?\n\r?)|(\n\r?(?=_))".toRegex())
    }

    private fun konverterNbsp(s: String): String {
        val utf8nonBreakingSpace = "\u00A0"
        val htmlNonBreakingSpace = "&nbsp;"
        return s.replace(utf8nonBreakingSpace.toRegex(), htmlNonBreakingSpace)
    }

    private fun ekstraLinjeskiftFørHilsing(s: String): String {
        return s.replace("<p>Med vennlig hilsen", "<p class=\"hilsen\">Med vennlig hilsen")
                .replace("<p>Med vennleg helsing", "<p class=\"hilsen\">Med vennleg helsing")
    }
}
