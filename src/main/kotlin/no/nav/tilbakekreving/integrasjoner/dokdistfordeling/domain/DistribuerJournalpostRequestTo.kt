package no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain

import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstidspunkt
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstype

data class DistribuerJournalpostRequestTo(
    val journalpostId: String,
    val batchId: String? = null,
    val bestillendeFagsystem: String,
    val adresse: AdresseTo? = null,
    val dokumentProdApp: String,
    val distribusjonstype: Distribusjonstype?,
    val distribusjonstidspunkt: Distribusjonstidspunkt,
)
