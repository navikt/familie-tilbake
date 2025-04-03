package no.nav.tilbakekreving.api.v2

data class FaktainfoDto(
    val revurderingsårsak: String,
    val revurderingsresultat: String,
    private val varsletBeløp: Long?,
)
