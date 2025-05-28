package no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf

import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmottager

data class Brevdata(
    var metadata: Brevmetadata,
    val tittel: String? = null,
    val overskrift: String,
    val mottager: Brevmottager,
    val brevtekst: String,
    val vedleggHtml: String = "",
)
