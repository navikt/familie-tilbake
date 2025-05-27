package no.nav.tilbakekreving.pdf.dokumentbestilling.felles.header

import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.pdf.sanitize
import no.nav.tilbakekreving.pdf.handlebars.FellesTekstformaterer

object TekstformatererHeader {
    fun lagHeader(
        brevmetadata: Brevmetadata,
        overskrift: String,
    ): String =
        lagHeader(
            HeaderData(
                språkkode = brevmetadata.språkkode,
                person = Person(brevmetadata.sakspartsnavn, brevmetadata.sakspartId),
                brev = Brev(overskrift),
                institusjon = if (brevmetadata.institusjon != null) Institusjon(brevmetadata.institusjon.organisasjonsnummer, sanitize(brevmetadata.institusjon.navn)) else null,
            ),
        )

    private fun lagHeader(data: HeaderData): String = FellesTekstformaterer.lagBrevtekst(data, "header")
}
