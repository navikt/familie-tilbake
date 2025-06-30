package no.nav.tilbakekreving.avstemming

class AvstemmingFilMapper(
    private val rader: List<Avstemmingsrad>,
) {
    fun tilFlatfil(): ByteArray = buildString {
        append(HEADER)
        rader.forEach { rad ->
            append(SKILLETEGN_RADER)
            append(rad.toCsvString())
        }
    }.toByteArray()

    companion object {
        private const val SKILLETEGN_RADER = "\n"

        val HEADER: String = buildString {
            listOf(
                "avsender",
                "vedtakId",
                "fnr",
                "vedtaksdato",
                "fagsakYtelseType",
                "tilbakekrevesBruttoUtenRenter",
                "skatt",
                "tilbakekrevesNettoUtenRenter",
                "renter",
                "erOmgjøringTilIngenTilbakekreving",
            ).joinTo(this, separator = Avstemmingsrad.SKILLETEGN_KOLONNER)
        }
    }
}
