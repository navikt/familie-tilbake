package no.nav.tilbakekreving.integrasjoner.dokument.kontrakter

data class Bruker(
    val id: String,
    val type: BrukerIdType,
)

enum class BrukerIdType {
    FNR,
}
