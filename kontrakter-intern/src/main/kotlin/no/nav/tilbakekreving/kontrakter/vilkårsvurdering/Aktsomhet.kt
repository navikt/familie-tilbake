package no.nav.tilbakekreving.kontrakter.vilkårsvurdering

enum class Aktsomhet(
    override val navn: String,
) : Vurdering {
    FORSETT("Forsett"),
    GROV_UAKTSOMHET("Grov uaktsomhet"),
    SIMPEL_UAKTSOMHET("Simpel uaktsomhet"),
}
