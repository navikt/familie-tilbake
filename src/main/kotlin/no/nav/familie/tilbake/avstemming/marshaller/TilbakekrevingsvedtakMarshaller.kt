package no.nav.familie.tilbake.avstemming.marshaller

import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakRequest
import java.io.StringReader
import java.io.StringWriter
import java.util.UUID
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.bind.Marshaller

object TilbakekrevingsvedtakMarshaller {

    private val context = JAXBContext.newInstance(TilbakekrevingsvedtakRequest::class.java)

    fun marshall(behandlingId: Long, request: TilbakekrevingsvedtakRequest?): String {
        //HAXX marshalling løses normalt sett ikke slik som dette. Se JaxbHelper for normaltilfeller.
        //HAXX gjør her marshalling uten kobling til skjema, siden skjema som brukes ikke er egnet for å
        //HAXX konvertere til streng. Skjemaet er bare egnet for å bruke mot WS.
        return try {
            val marshaller = context!!.createMarshaller()
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false)
            val stringWriter = StringWriter()
            marshaller.marshal(request, stringWriter)
            stringWriter.toString()
        } catch (e: JAXBException) {
            error("Kunne ikke marshalle vedtak for behandlingId=$behandlingId")
        }
    }

    fun unmarshall(xml: String, xmlId: UUID, behandlingId: UUID): TilbakekrevingsvedtakRequest {
        return try {
            val unmarshaller = context!!.createUnmarshaller()
            unmarshaller.unmarshal(StringReader(xml)) as TilbakekrevingsvedtakRequest
        } catch (e: JAXBException) {
            throw error("Kunne ikke unmarshalle vedtak for behandlingId=$xmlId xmlId=$behandlingId")
        }
    }
}