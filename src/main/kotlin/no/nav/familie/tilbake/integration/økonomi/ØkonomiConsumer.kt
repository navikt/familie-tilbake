package no.nav.familie.tilbake.integration.økonomi

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.tilbake.common.exceptionhandler.IntegrasjonException
import no.nav.familie.tilbake.common.exceptionhandler.SperretKravgrunnlagFeil
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagHentDetaljRequest
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingPortType
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakRequest
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakResponse
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.typer.v1.MmelDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.util.UUID
import javax.xml.ws.soap.SOAPFaultException

interface ØkonomiConsumer {

    fun iverksettVedtak(behandlingId: UUID, tilbakekrevingsvedtakRequest: TilbakekrevingsvedtakRequest)
            : TilbakekrevingsvedtakResponse
    fun hentKravgrunnlag(kravgrunnlagId: BigInteger, hentKravgrunnlagRequest: KravgrunnlagHentDetaljRequest)
            : DetaljertKravgrunnlagDto
}

@Service
@Profile("!e2e")
class DefaultØkonomiConsumer(private val økonomiService: TilbakekrevingPortType): ØkonomiConsumer {

    private val logger: Logger = LoggerFactory.getLogger(this::class.java)

    override fun iverksettVedtak(behandlingId: UUID, tilbakekrevingsvedtakRequest: TilbakekrevingsvedtakRequest)
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

        } catch (exception: SOAPFaultException) {
            logger.error("tilbakekrevingsvedtak kan ikke sende til økonomi for behandling=$behandlingId. " +
                         "Feiler med ${exception.message}")
            throw IntegrasjonException(msg = "Fikk feil fra økonomi ved iverksetting av behandling=$behandlingId",
                                       throwable = exception)
        }
    }

    override fun hentKravgrunnlag(kravgrunnlagId: BigInteger, hentKravgrunnlagRequest: KravgrunnlagHentDetaljRequest)
            : DetaljertKravgrunnlagDto {
        logger.info("Henter kravgrunnlag fra økonomi for kravgrunnlagId=$kravgrunnlagId")
        try {
            val respons = økonomiService.kravgrunnlagHentDetalj(hentKravgrunnlagRequest)
            validerHentKravgrunnlagRespons(respons.mmel, kravgrunnlagId)
            logger.info("Mottatt respons: ${lagRespons(respons.mmel)} fra økonomi til kravgrunnlagId=$kravgrunnlagId.")
            return respons.detaljertkravgrunnlag
        } catch (exception: SOAPFaultException) {
            logger.error("Kravgrunnlag kan ikke hentes fra økonomi for behandling=$kravgrunnlagId. " +
                         "Feiler med ${exception.message}")
            throw IntegrasjonException(msg = "Kravgrunnlag kan ikke hentes fra økonomi for kravgrunnlagId=$kravgrunnlagId",
                                       throwable = exception)
        }
    }

    private fun validerHentKravgrunnlagRespons(mmelDto: MmelDto, kravgrunnlagId: BigInteger) {
        if (!erResponsOk(mmelDto) || erKravgrunnlagIkkeFinnes(mmelDto)) {
            logger.error("Fikk feil respons:${lagRespons(mmelDto)} fra økonomi ved henting av kravgrunnlag " +
                         "for kravgrunnlagId=$kravgrunnlagId.")
            throw IntegrasjonException(msg = "Fikk feil respons:${lagRespons(mmelDto)} fra økonomi " +
                                             "ved henting av kravgrunnlag for kravgrunnlagId=$kravgrunnlagId.")
        } else if (erKravgrunnlagSperret(mmelDto)) {
            logger.warn("Hentet kravgrunnlag for kravgrunnlagId=$kravgrunnlagId er sperret")
            throw SperretKravgrunnlagFeil(melding = "Hentet kravgrunnlag for kravgrunnlagId=$kravgrunnlagId er sperret")
        }
    }

    private fun erResponsOk(mmelDto: MmelDto): Boolean {
        return mmelDto.alvorlighetsgrad in setOf("00", "04")
    }

    private fun erKravgrunnlagSperret(mmelDto: MmelDto): Boolean {
        return KODE_MELDING_SPERRET_KRAVGRUNNLAG.equals(mmelDto.kodeMelding)
    }

    private fun erKravgrunnlagIkkeFinnes(mmelDto: MmelDto): Boolean {
        return KODE_MELDING_KRAVGRUNNLAG_IKKE_FINNES.equals(mmelDto.kodeMelding)
    }

    private fun lagRespons(mmelDto: MmelDto): String {
        return objectMapper.writeValueAsString(mmelDto)
    }

    companion object {

        const val KODE_MELDING_SPERRET_KRAVGRUNNLAG = "B420012I"
        const val KODE_MELDING_KRAVGRUNNLAG_IKKE_FINNES = "B420010I"
    }
}

@Service
@Profile("e2e")
class E2EØkonomiConsumer : ØkonomiConsumer {

    override fun iverksettVedtak(behandlingId: UUID,
                                 tilbakekrevingsvedtakRequest: TilbakekrevingsvedtakRequest): TilbakekrevingsvedtakResponse {
        logger.info("Skipper arkivering av dokument i e2e-profil")
        val mmelDto = MmelDto()
        mmelDto.alvorlighetsgrad = "00"
        val response = TilbakekrevingsvedtakResponse()
        response.mmel = mmelDto
        return response
    }

    override fun hentKravgrunnlag(kravgrunnlagId: BigInteger,
                                  hentKravgrunnlagRequest: KravgrunnlagHentDetaljRequest): DetaljertKravgrunnlagDto {
        TODO("Not yet implemented")
    }

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
