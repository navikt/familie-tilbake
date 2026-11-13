package no.nav.tilbakekreving.integrasjoner.tokenexchange.kontrakter

data class TokenIntrospectionResponse(
    val active: Boolean,
    val idtyp: String?,
    val roles: List<String>?,
)
