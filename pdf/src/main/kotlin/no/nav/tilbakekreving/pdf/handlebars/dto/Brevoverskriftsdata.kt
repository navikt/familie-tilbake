package no.nav.tilbakekreving.pdf.handlebars.dto

import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata

data class Brevoverskriftsdata(
    val brevmetadata: Brevmetadata,
) : BaseDokument(
        brevmetadata.ytelsestype,
        brevmetadata.språkkode,
        brevmetadata.behandlendeEnhetsNavn,
        brevmetadata.ansvarligSaksbehandler,
        brevmetadata.gjelderDødsfall,
        brevmetadata.institusjon,
    )
