package no.nav.tilbakekreving.integrasjoner.dokdistfordeling.domain

import kotlinx.serialization.Serializable
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstidspunkt
import no.nav.familie.tilbake.kontrakter.dokdist.Distribusjonstype

@Serializable
data class DistribuerJournalpostRequestTo(
    val journalpostId: String,
    val batchId: String? = null,
    val bestillendeFagsystem: String,
    val adresse: AdresseTo? = null,
    val dokumentProdApp: String,
    val distribusjonstype: Distribusjonstype?,
    val distribusjonstidspunkt: Distribusjonstidspunkt,
)
