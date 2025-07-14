package no.nav.tilbakekreving.burdeForstått

import jakarta.xml.bind.JAXBContext
import jakarta.xml.bind.Marshaller
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.TilbakekrevingService
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagBufferRepository
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagMelding
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.io.StringWriter
import java.util.UUID
import java.util.concurrent.TimeoutException

@Service
class BurdeForståttService(
    private val kravgrunnlagBufferRepository: KravgrunnlagBufferRepository,
    private val tilbakekrevingService: TilbakekrevingService,
) {
    fun leggTilKravgrunnlag(kravgrunnlag: DetaljertKravgrunnlagDto): UUID {
        val melding = DetaljertKravgrunnlagMelding().apply {
            detaljertKravgrunnlag = kravgrunnlag
        }

        val jaxbContext = JAXBContext.newInstance(
            DetaljertKravgrunnlagMelding::class.java,
            DetaljertKravgrunnlagDto::class.java,
        )
        val marshaller = jaxbContext.createMarshaller()
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true)
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "utf-8")

        val writer = StringWriter()
        marshaller.marshal(melding, writer)
        val kravgrunnlagXML = writer.toString()

        val kravgrunnlagId = "BF" + kravgrunnlag.kravgrunnlagId.toString()

        kravgrunnlagBufferRepository.lagre(KravgrunnlagBufferRepository.Entity(kravgrunnlagXML, kravgrunnlagId))
        val fagsystem = when (kravgrunnlag.kodeFagomraade) {
            "TILLST" -> Ytelse.Tilleggsstønad
            else -> throw Feil(
                message = "Kan ikke håndtere saker for ${kravgrunnlag.kodeFagomraade} med ny modell",
                httpStatus = HttpStatus.BAD_REQUEST,
                logContext = SecureLog.Context.utenBehandling(kravgrunnlag.fagsystemId),
            )
        }

        val timeoutMillis = 60_000L
        val pollIntervalMillis = 1000L
        val startTime = System.currentTimeMillis()

        var tilbakekreving: Tilbakekreving?

        do {
            tilbakekreving = tilbakekrevingService.hentTilbakekreving(
                fagsystem.tilFagsystemDTO(),
                kravgrunnlag.fagsystemId,
            )

            if (tilbakekreving == null) {
                if (System.currentTimeMillis() - startTime > timeoutMillis) {
                    throw TimeoutException("Timed out etter 1 minute mens venter på tilbakekreving blir opprettet.")
                }
                Thread.sleep(pollIntervalMillis)
            }
        } while (tilbakekreving == null)

        return tilbakekreving.behandlingHistorikk.nåværende().entry.internId
    }
}
