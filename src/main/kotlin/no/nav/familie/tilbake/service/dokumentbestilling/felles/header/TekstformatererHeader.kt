package no.nav.familie.tilbake.service.dokumentbestilling.felles.header

import com.github.jknack.handlebars.Template
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.FellesTekstformaterer
import java.io.IOException

object TekstformatererHeader {

    fun lagHeader(brevmetadata: Brevmetadata, overskrift: String): String? {
        return lagHeader(HeaderData(brevmetadata.språkkode,
                                    Person(brevmetadata.sakspartsnavn,
                                           brevmetadata.sakspartId),
                                    Brev(overskrift)))
    }

    private fun lagHeader(data: HeaderData): String? {
        return try {
            val template: Template = FellesTekstformaterer.opprettHandlebarsTemplate("header", data.språkkode)
            FellesTekstformaterer.applyTemplate(template, data)
        } catch (e: IOException) {
            throw IllegalStateException("Feil ved tekstgenerering.")
        }
    }

}
