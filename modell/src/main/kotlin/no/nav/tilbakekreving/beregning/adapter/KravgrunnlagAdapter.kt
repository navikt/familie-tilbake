package no.nav.tilbakekreving.beregning.adapter

interface KravgrunnlagAdapter {
    fun perioder(): List<KravgrunnlagPeriodeAdapter>
}
