package no.nav.tilbakekreving.kontrakter.vilkårsvurdering

enum class AnnenVurdering(
    override val navn: String,
) : Vurdering {
    GOD_TRO("Handlet i god tro"),
    FORELDET("Foreldet"),
}
