package no.nav.tilbakekreving.kontrakter.behandling

@Schema(name = "Behandlingsstatus")
enum class Behandlingsstatus(
    val kode: String,
) {
    AVSLUTTET("AVSLU"),
    FATTER_VEDTAK("FVED"),
    IVERKSETTER_VEDTAK("IVED"),
    OPPRETTET("OPPRE"),
    UTREDES("UTRED"),
}
