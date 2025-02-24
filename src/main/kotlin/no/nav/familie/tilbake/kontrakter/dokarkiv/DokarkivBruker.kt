package no.nav.familie.tilbake.kontrakter.dokarkiv

import no.nav.familie.tilbake.kontrakter.BrukerIdType

data class DokarkivBruker(
    val idType: BrukerIdType,
    val id: String,
)
