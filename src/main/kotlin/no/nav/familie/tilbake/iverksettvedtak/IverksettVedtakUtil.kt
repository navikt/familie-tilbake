package no.nav.familie.tilbake.iverksettvedtak

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakRequest
import java.io.StringReader
import java.io.StringWriter
import java.util.UUID
import javax.xml.bind.JAXBContext
import javax.xml.bind.JAXBException
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller

object IverksettVedtakUtil {

    private val jaxbContext: JAXBContext = JAXBContext.newInstance(TilbakekrevingsvedtakRequest::class.java)

    fun marshalIverksettVedtakRequest(behandlingId: UUID,
                                      tilbakekrevingsvedtakRequest: TilbakekrevingsvedtakRequest): String {
        return try {
            val jaxbMarshaller: Marshaller = jaxbContext.createMarshaller()

            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false)
            val stringWriter = StringWriter()
            jaxbMarshaller.marshal(tilbakekrevingsvedtakRequest, stringWriter)

            stringWriter.toString()
        } catch (exception: JAXBException) {
            throw Feil(message = "Kunne ikke marshalle iverksettvedtak request for behandlingId=$behandlingId",
                       throwable = exception)
        }


    }

    // kun brukes i test
    fun unmarshallIverksettVedtakRequest(requestXml: String): TilbakekrevingsvedtakRequest {
        return try {
            val jaxbUnmarshaller: Unmarshaller = jaxbContext.createUnmarshaller()

            (jaxbUnmarshaller.unmarshal(StringReader(requestXml)) as TilbakekrevingsvedtakRequest)
        } catch (exception: JAXBException) {
            throw Feil(message = "Kunne ikke unmarshalle requestXml=$requestXml", throwable = exception)
        }
    }
}
