package no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain

import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstidspunkt
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstype

data class DistribuerJournalpostRequest(
    val journalpostId: String,
    val batchId: String?,
    val bestillendeFagsystem: String,
    val adresse: AdresseTo?,
    val dokumentProdApp: String,
    val distribusjonstype: Distribusjonstype?,
    val distribusjonstidspunkt: Distribusjonstidspunkt,
)
