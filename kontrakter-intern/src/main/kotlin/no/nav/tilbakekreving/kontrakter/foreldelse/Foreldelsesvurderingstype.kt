package no.nav.tilbakekreving.kontrakter.foreldelse

enum class Foreldelsesvurderingstype(
    val navn: String,
) {
    IKKE_VURDERT("Perioden er ikke vurdert"),
    FORELDET("Perioden er foreldet"),
    IKKE_FORELDET("Perioden er ikke foreldet"),
    TILLEGGSFRIST("Perioden er ikke foreldet, regel om tilleggsfrist (10 år) benyttes"),
}
