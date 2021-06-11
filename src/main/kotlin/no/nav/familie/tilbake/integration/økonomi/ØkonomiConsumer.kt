package no.nav.familie.tilbake.integration.økonomi

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.tilbake.common.exceptionhandler.IntegrasjonException
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingPortType
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakRequest
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakResponse
import no.nav.tilbakekreving.typer.v1.MmelDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.remoting.soap.SoapFaultException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ØkonomiConsumer(private val økonomiService: TilbakekrevingPortType) {

    private val logger: Logger = LoggerFactory.getLogger(this.javaClass)

    fun iverksettVedtak(behandlingId: UUID, tilbakekrevingsvedtakRequest: TilbakekrevingsvedtakRequest)
            : TilbakekrevingsvedtakResponse {
        logger.info("Sender tilbakekrevingsvedtak til økonomi for behandling $behandlingId")
        try {
            val respons = økonomiService.tilbakekrevingsvedtak(tilbakekrevingsvedtakRequest)
            if (!erResponsOk(respons.mmel)) {
                logger.error("Fikk feil respons fra økonomi ved iverksetting av behandling=$behandlingId." +
                             "Mottatt respons:${lagRespons(respons.mmel)}")
                throw IntegrasjonException(msg = "Fikk feil respons fra økonomi ved iverksetting av behandling=$behandlingId." +
                                                 "Mottatt respons:${lagRespons(respons.mmel)}")
            }
            logger.info("Mottatt respons: ${lagRespons(respons.mmel)} fra økonomi ved iverksetting av behandling=$behandlingId.")
            return respons

        } catch (exception: SoapFaultException) {
            logger.error("tilbakekrevingsvedtak kan ikke sende til økonomi for behandling=$behandlingId. " +
                         "Feiler med ${exception.message}")
            throw IntegrasjonException(msg = "Fikk feil fra økonomi ved iverksetting av behandling=$behandlingId",
                                       throwable = exception)
        }
    }

    private fun erResponsOk(mmelDto: MmelDto): Boolean {
        return mmelDto.alvorlighetsgrad in setOf("00", "04")
    }

    private fun lagRespons(mmelDto: MmelDto): String {
        return objectMapper.writeValueAsString(mmelDto)
    }
}
