package no.nav.familie.tilbake.brev.dokumentbestilling.fritekstbrev

import no.nav.familie.tilbake.brev.dokumentbestilling.felles.BrevMetadata

class FritekstbrevData(val tittel: String? = null,
                       val overskrift: String,
                       val brevtekst: String,
                       val brevMetadata: BrevMetadata)
