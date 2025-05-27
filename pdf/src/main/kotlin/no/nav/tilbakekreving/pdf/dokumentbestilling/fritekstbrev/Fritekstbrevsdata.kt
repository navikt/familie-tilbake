package no.nav.tilbakekreving.pdf.dokumentbestilling.fritekstbrev

import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata

class Fritekstbrevsdata(
    val overskrift: String,
    val brevtekst: String,
    val brevmetadata: Brevmetadata,
)
