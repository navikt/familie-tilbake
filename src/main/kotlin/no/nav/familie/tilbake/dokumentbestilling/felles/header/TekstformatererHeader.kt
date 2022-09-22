package no.nav.familie.tilbake.dokumentbestilling.felles.header

import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.handlebars.FellesTekstformaterer

object TekstformatererHeader {

    fun lagHeader(brevmetadata: Brevmetadata, overskrift: String, organisasjonsnavn: String? = null): String {
        return lagHeader(
            HeaderData(
                språkkode = brevmetadata.språkkode,
                person = Person(brevmetadata.sakspartsnavn, brevmetadata.sakspartId),
                brev = Brev(overskrift),
                institusjon = brevmetadata.institusjon?.let {
                    Institusjon(
                        organisasjonsnummer = it.organisasjonsnummer,
                        navn = organisasjonsnavn!!
                    )
                }
            )
        )
    }

    private fun lagHeader(data: HeaderData): String {
        return FellesTekstformaterer.lagBrevtekst(data, "header")
    }
}
