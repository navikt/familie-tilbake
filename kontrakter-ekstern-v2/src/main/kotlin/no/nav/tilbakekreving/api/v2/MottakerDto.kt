package no.nav.tilbakekreving.api.v2

data class MottakerDto(
    val ident: String,
    val type: MottakerType,
) {
    enum class MottakerType {
        PERSON,
    }
}
