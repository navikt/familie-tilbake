package no.nav.familie.tilbake.kontrakter.simulering

data class SimuleringMottaker(
    val simulertPostering: List<SimulertPostering>, // perioder
    val mottakerNummer: String? = null,
    val mottakerType: MottakerType,
) {
    override fun toString(): String =
        (
            javaClass.simpleName +
                "< mottakerType=" + mottakerType +
                ">"
        )
}
