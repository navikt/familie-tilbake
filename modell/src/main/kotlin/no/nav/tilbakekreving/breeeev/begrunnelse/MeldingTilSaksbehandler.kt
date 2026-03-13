package no.nav.tilbakekreving.breeeev.begrunnelse

enum class MeldingTilSaksbehandler(
    val melding: String,
    val gjelderVurderinger: Array<VilkårsvurderingBegrunnelse>,
    private val erForPeriodeavsnitt: Boolean,
) {
    BEGRUNN_BRUKERS_UTTALELSE(
        melding = "Husk å vurdere uttalelsen til bruker",
        erForPeriodeavsnitt = true,
        gjelderVurderinger = arrayOf(
            VilkårsvurderingBegrunnelse.TILBAKEKREVES,
            VilkårsvurderingBegrunnelse.INGEN_TILBAKEKREVING,
        ),
    ),
    ;

    companion object {
        fun Iterable<MeldingTilSaksbehandler>.forPeriodeavsnitt() = filter { it.erForPeriodeavsnitt }

        fun Iterable<MeldingTilSaksbehandler>.forBegrunnelse(begrunnelse: VilkårsvurderingBegrunnelse) = filter { begrunnelse in it.gjelderVurderinger }
    }
}
