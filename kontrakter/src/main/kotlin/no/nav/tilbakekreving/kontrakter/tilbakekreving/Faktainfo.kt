package no.nav.tilbakekreving.kontrakter.tilbakekreving

data class Faktainfo(
    val revurderingsårsak: String,
    val revurderingsresultat: String,
    val tilbakekrevingsvalg: Tilbakekrevingsvalg? = null,
    val konsekvensForYtelser: Set<String> = emptySet(),
)

enum class Tilbakekrevingsvalg {
    OPPRETT_TILBAKEKREVING_MED_VARSEL,
    OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
    OPPRETT_TILBAKEKREVING_AUTOMATISK,
    IGNORER_TILBAKEKREVING,
}
