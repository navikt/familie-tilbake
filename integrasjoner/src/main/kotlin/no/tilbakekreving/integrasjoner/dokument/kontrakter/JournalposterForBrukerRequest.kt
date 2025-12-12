package no.tilbakekreving.integrasjoner.dokument.kontrakter

data class JournalposterForBrukerRequest(
    val brukerId: Bruker,
    val antall: Int,
    val tema: List<IntegrasjonTema>? = null,
    val journalposttype: List<Journalposttype>? = null,
)
