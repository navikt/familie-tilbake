package no.nav.familie.tilbake.kontrakter.dokarkiv

import no.nav.familie.tilbake.kontrakter.journalpost.AvsenderMottakerIdType

data class AvsenderMottaker(
    val id: String?,
    val idType: AvsenderMottakerIdType?,
    val navn: String,
)
