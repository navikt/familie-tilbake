package no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf

import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevMetadata
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevMottaker

data class BrevData(var metadata: BrevMetadata,
                    val tittel: String? = null,
                    val overskrift: String,
                    val mottaker: BrevMottaker,
                    val brevtekst: String,
                    val vedleggHtml: String? = null)

