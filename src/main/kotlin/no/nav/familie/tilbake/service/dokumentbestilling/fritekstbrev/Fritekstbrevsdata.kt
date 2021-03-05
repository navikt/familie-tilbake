package no.nav.familie.tilbake.service.dokumentbestilling.fritekstbrev

import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmetadata

class Fritekstbrevsdata(val tittel: String? = null,
                        val overskrift: String,
                        val brevtekst: String,
                        val brevmetadata: Brevmetadata)
