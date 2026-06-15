package no.nav.tilbakekreving.kontrakter.foreldelse

enum class Foreldelsesvurderingstype(
    val navn: String,
) {
    IKKE_VURDERT("Perioden er ikke vurdert"),
    AUTOMATISK_VURDERT_IKKE_FORELDET("Perioden er automatisk vurdert, ikke foreldet"),
    FORELDET("Perioden er foreldet"),
    IKKE_FORELDET("Perioden er ikke foreldet"),
    TILLEGGSFRIST("Perioden er ikke foreldet, regel om tilleggsfrist (10 år) benyttes"),
}
