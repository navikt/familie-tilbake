package no.nav.familie.tilbake.dokumentbestilling.felles.header

import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.handlebars.FellesTekstformaterer

object TekstformatererHeader {

    fun lagHeader(brevmetadata: Brevmetadata, overskrift: String): String {
        return lagHeader(HeaderData(brevmetadata.språkkode,
                                    Person(brevmetadata.sakspartsnavn, brevmetadata.sakspartId),
                                    Brev(overskrift)))
    }

    private fun lagHeader(data: HeaderData): String {
        return FellesTekstformaterer.lagBrevtekst(data, "header")
    }

}
