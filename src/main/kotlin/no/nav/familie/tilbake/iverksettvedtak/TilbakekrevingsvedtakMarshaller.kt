package no.nav.familie.tilbake.iverksettvedtak

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.JAXBException
import jakarta.xml.bind.Marshaller
import jakarta.xml.bind.Unmarshaller
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakRequest
import java.io.StringReader
import java.io.StringWriter
import java.util.UUID

object TilbakekrevingsvedtakMarshaller {
    private val context = JAXBContext.newInstance(TilbakekrevingsvedtakRequest::class.java)

    fun marshall(
        behandlingId: UUID,
        request: TilbakekrevingsvedtakRequest,
        logContext: SecureLog.Context,
    ): String =
        try {
            val marshaller = context.createMarshaller()
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false)
            val stringWriter = StringWriter()
            marshaller.marshal(request, stringWriter)

            stringWriter.toString()
        } catch (e: JAXBException) {
            throw Feil("Kunne ikke marshalle TilbakekrevingsvedtakRequest for behandlingId=$behandlingId", logContext = logContext, throwable = e)
        }

    fun unmarshall(
        xml: String,
        behandlingId: UUID,
        xmlId: UUID,
        logContext: SecureLog.Context,
    ): TilbakekrevingsvedtakRequest =
        try {
            val unmarshaller: Unmarshaller = context.createUnmarshaller()

            (unmarshaller.unmarshal(StringReader(xml)) as TilbakekrevingsvedtakRequest)
        } catch (e: JAXBException) {
            throw Feil("Kunne ikke unmarshalle requestXml=$xml med id=$xmlId for behandling=$behandlingId", logContext = logContext, throwable = e)
        }
}
