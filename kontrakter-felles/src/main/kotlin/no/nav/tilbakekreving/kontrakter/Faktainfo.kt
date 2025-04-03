package no.nav.tilbakekreving.kontrakter

data class Faktainfo(
    val revurderingsårsak: String,
    val revurderingsresultat: String,
    val tilbakekrevingsvalg: Tilbakekrevingsvalg? = null,
    val konsekvensForYtelser: Set<String> = emptySet(),
    val varsletBeløp: Long? = null,
)

enum class Tilbakekrevingsvalg {
    OPPRETT_TILBAKEKREVING_MED_VARSEL,
    OPPRETT_TILBAKEKREVING_UTEN_VARSEL,
    OPPRETT_TILBAKEKREVING_AUTOMATISK,
    IGNORER_TILBAKEKREVING,
}
