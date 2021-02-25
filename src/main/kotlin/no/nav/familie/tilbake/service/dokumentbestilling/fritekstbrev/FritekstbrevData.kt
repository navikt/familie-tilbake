package no.nav.familie.tilbake.service.dokumentbestilling.fritekstbrev

import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevMetadata

class FritekstbrevData(val tittel: String? = null,
                       val overskrift: String,
                       val brevtekst: String,
                       val brevMetadata: BrevMetadata)
