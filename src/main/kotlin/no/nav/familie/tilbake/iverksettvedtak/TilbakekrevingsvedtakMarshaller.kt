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

object TilbakekrevingsvedtakMarshaller {

    private val context = JAXBContext.newInstance(TilbakekrevingsvedtakRequest::class.java)

    fun marshall(behandlingId: UUID, request: TilbakekrevingsvedtakRequest): String {
        return try {
            val marshaller = context.createMarshaller()
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false)
            val stringWriter = StringWriter()
            marshaller.marshal(request, stringWriter)

            stringWriter.toString()
        } catch (e: JAXBException) {
            throw Feil("Kunne ikke marshalle TilbakekrevingsvedtakRequest for behandlingId=$behandlingId", e)
        }
    }

    fun unmarshall(xml: String, behandlingId: UUID, xmlId: UUID): TilbakekrevingsvedtakRequest {
        return try {
            val unmarshaller: Unmarshaller = context.createUnmarshaller()

            (unmarshaller.unmarshal(StringReader(xml)) as TilbakekrevingsvedtakRequest)
        } catch (e: JAXBException) {
            throw Feil("Kunne ikke unmarshalle requestXml=$xml med id=$xmlId for behandling=$behandlingId", e)
        }
    }
}
