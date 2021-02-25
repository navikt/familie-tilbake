package no.nav.familie.tilbake.service.dokumentbestilling.felles.header

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Template
import no.nav.familie.kontrakter.felles.tilbakekreving.Språkkode
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevMetadata
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.FellesTekstformaterer
import java.io.IOException

object TekstformatererHeader {

    fun lagHeader(brevMetadata: BrevMetadata, overskrift: String): String? {
        return lagHeader(HeaderData(brevMetadata.språkkode,
                                    Person(brevMetadata.sakspartNavn,
                                           brevMetadata.sakspartId),
                                    Brev(overskrift)))
    }

    private fun lagHeader(data: HeaderData): String? {
        return try {
            val template: Template = opprettHandlebarsTemplate("header", data.språkkode)
            FellesTekstformaterer.applyTemplate(template, data)
        } catch (e: IOException) {
            throw IllegalStateException("Feil ved tekstgenerering.")
        }
    }

    private fun opprettHandlebarsTemplate(filsti: String, språkkode: Språkkode): Template {
        val handlebars: Handlebars = FellesTekstformaterer.opprettHandlebarsKonfigurasjon()
        return handlebars.compile(FellesTekstformaterer.lagSpråkstøttetFilsti(filsti, språkkode))
    }
}