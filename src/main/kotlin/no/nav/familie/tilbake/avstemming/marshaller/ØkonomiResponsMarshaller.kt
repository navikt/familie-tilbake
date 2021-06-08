package no.nav.familie.tilbake.avstemming.marshaller

import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakResponse
import java.io.StringReader
import java.io.StringWriter
import java.util.UUID
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.bind.Marshaller

object ØkonomiResponsMarshaller {

    @get:Throws(JAXBException::class) @Volatile
    private var context: JAXBContext? = null
        private get() {
            if (field == null) {
                field = JAXBContext.newInstance(TilbakekrevingsvedtakResponse::class.java)
            }
            return field
        }

    fun marshall(respons: TilbakekrevingsvedtakResponse, behandlingId: UUID): String {
        //HAXX marshalling løses normalt sett ikke slik som dette. Se JaxbHelper for normaltilfeller.
        //HAXX gjør her marshalling uten kobling til skjema, siden skjema som brukes ikke er egnet for å
        //HAXX konvertere til streng. Skjemaet er bare egnet for å bruke mot WS.
        return try {
            val marshaller = context!!.createMarshaller()
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false)
            val stringWriter = StringWriter()
            marshaller.marshal(respons, stringWriter)
            stringWriter.toString()
        } catch (e: JAXBException) {
            throw error("Kunne ikke marshalle respons fra økonomi for behandlingId=$behandlingId")
        }
    }

    fun unmarshall(xml: String, behandlingId: UUID, xmlId: UUID): TilbakekrevingsvedtakResponse {
        return try {
            val unmarshaller = context!!.createUnmarshaller()
            unmarshaller.unmarshal(StringReader(xml)) as TilbakekrevingsvedtakResponse
        } catch (e: JAXBException) {
            throw error("Kunne ikke unmarshalle respons fra økonomi for behandlingId=$behandlingId xmlId=$xmlId")
        }
    }
}