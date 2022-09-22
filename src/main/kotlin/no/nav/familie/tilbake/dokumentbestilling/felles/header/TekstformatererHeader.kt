package no.nav.familie.tilbake.dokumentbestilling.felles.header

import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.handlebars.FellesTekstformaterer
import no.nav.familie.tilbake.organisasjon.OrganisasjonService

object TekstformatererHeader {

    fun lagHeader(brevmetadata: Brevmetadata, overskrift: String, organisasjonService: OrganisasjonService): String {
        return lagHeader(
            HeaderData(
                språkkode = brevmetadata.språkkode,
                person = Person(brevmetadata.sakspartsnavn, brevmetadata.sakspartId),
                brev = Brev(overskrift),
                institusjon = brevmetadata.institusjon?.let {
                    Institusjon(
                        organisasjonsnummer = it.organisasjonsnummer,
                        navn = organisasjonService.hentOrganisasjonNavn(it.organisasjonsnummer)
                    )
                }
            )
        )
    }

    private fun lagHeader(data: HeaderData): String {
        return FellesTekstformaterer.lagBrevtekst(data, "header")
    }
}
