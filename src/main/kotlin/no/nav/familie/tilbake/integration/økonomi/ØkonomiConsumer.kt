package no.nav.familie.tilbake.integration.økonomi

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.tilbake.common.exceptionhandler.IntegrasjonException
import no.nav.familie.tilbake.common.exceptionhandler.SperretKravgrunnlagFeil
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Fagområdekode
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravstatuskode
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagHentDetaljRequest
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagHentDetaljResponse
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingPortType
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakRequest
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakResponse
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagBelopDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagPeriodeDto
import no.nav.tilbakekreving.typer.v1.MmelDto
import no.nav.tilbakekreving.typer.v1.PeriodeDto
import no.nav.tilbakekreving.typer.v1.TypeGjelderDto
import no.nav.tilbakekreving.typer.v1.TypeKlasseDto
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
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
class DefaultØkonomiConsumer(private val økonomiService: TilbakekrevingPortType) : ØkonomiConsumer {

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
class E2EØkonomiConsumer(private val kravgrunnlagRepository: KravgrunnlagRepository) : ØkonomiConsumer {

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
        logger.info("Henter kravgrunnlag fra økonomi for kravgrunnlagId=$kravgrunnlagId")
        val respons = kravgrunnlagHentDetalj(hentKravgrunnlagRequest)
        validerHentKravgrunnlagRespons(respons.mmel, kravgrunnlagId)
        logger.info("Mottatt respons: ${lagRespons(respons.mmel)} fra økonomi til kravgrunnlagId=$kravgrunnlagId.")
        return respons.detaljertkravgrunnlag
    }

    fun kravgrunnlagHentDetalj(request: KravgrunnlagHentDetaljRequest): KravgrunnlagHentDetaljResponse {
        val hentKravgrunnlagRequest = request.hentkravgrunnlag
        val eksisterendeKravgrunnlag =
                kravgrunnlagRepository.findByEksternKravgrunnlagIdAndAktivIsTrue(hentKravgrunnlagRequest.kravgrunnlagId)

        val respons = KravgrunnlagHentDetaljResponse()
        respons.mmel = lagMmelDto()

        respons.detaljertkravgrunnlag = DetaljertKravgrunnlagDto().apply {
            kravgrunnlagId = hentKravgrunnlagRequest.kravgrunnlagId
            enhetAnsvarlig = hentKravgrunnlagRequest.enhetAnsvarlig
            enhetBehandl = hentKravgrunnlagRequest.enhetAnsvarlig
            enhetBosted = hentKravgrunnlagRequest.enhetAnsvarlig
            saksbehId = hentKravgrunnlagRequest.saksbehId
            kodeFagomraade = Fagområdekode.BA.name
            vedtakId = eksisterendeKravgrunnlag?.vedtakId ?: BigInteger.ZERO
            kodeStatusKrav = Kravstatuskode.NYTT.kode
            fagsystemId = eksisterendeKravgrunnlag?.fagsystemId ?: "0"
            datoVedtakFagsystem = eksisterendeKravgrunnlag?.fagsystemVedtaksdato ?: LocalDate.now()
            vedtakIdOmgjort = eksisterendeKravgrunnlag?.omgjortVedtakId ?: BigInteger.ZERO
            vedtakGjelderId = eksisterendeKravgrunnlag?.gjelderVedtakId ?: "1234"
            typeGjelderId = TypeGjelderDto.PERSON
            utbetalesTilId = eksisterendeKravgrunnlag?.utbetalesTilId ?: "1234"
            typeUtbetId = TypeGjelderDto.PERSON
            kontrollfelt = eksisterendeKravgrunnlag?.kontrollfelt ?: LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("YYYY-MM-dd-HH.mm.ss.SSSSS"))
            referanse = eksisterendeKravgrunnlag?.referanse ?: "0"
            tilbakekrevingsPeriode.addAll(mapPeriode(eksisterendeKravgrunnlag?.perioder!!))
        }
        return respons
    }

    private fun lagMmelDto(): MmelDto {
        val mmelDto = MmelDto()
        mmelDto.alvorlighetsgrad = "00"
        mmelDto.kodeMelding = "OK"
        return mmelDto
    }

    private fun mapPeriode(perioder: Set<Kravgrunnlagsperiode432>): List<DetaljertKravgrunnlagPeriodeDto> {
        return perioder.map {
            DetaljertKravgrunnlagPeriodeDto().apply {
                periode = PeriodeDto().apply {
                    fom = it.periode.fomDato
                    tom = it.periode.tomDato
                }
                belopSkattMnd = it.månedligSkattebeløp
                tilbakekrevingsBelop.addAll(mapBeløp(it.beløp))
            }
        }
    }

    private fun mapBeløp(beløper: Set<Kravgrunnlagsbeløp433>): List<DetaljertKravgrunnlagBelopDto> {
        return beløper.map {
            DetaljertKravgrunnlagBelopDto().apply {
                kodeKlasse = it.klassekode.name
                typeKlasse = TypeKlasseDto.fromValue(it.klassetype.name)
                belopNy = it.nyttBeløp
                belopOpprUtbet = it.opprinneligUtbetalingsbeløp
                belopUinnkrevd = it.uinnkrevdBeløp
                belopTilbakekreves = it.tilbakekrevesBeløp
                skattProsent = it.skatteprosent
            }
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
        return DefaultØkonomiConsumer.KODE_MELDING_SPERRET_KRAVGRUNNLAG.equals(mmelDto.kodeMelding)
    }

    private fun erKravgrunnlagIkkeFinnes(mmelDto: MmelDto): Boolean {
        return DefaultØkonomiConsumer.KODE_MELDING_KRAVGRUNNLAG_IKKE_FINNES.equals(mmelDto.kodeMelding)
    }

    private fun lagRespons(mmelDto: MmelDto): String {
        return objectMapper.writeValueAsString(mmelDto)
    }

    companion object {

        private val logger: Logger = LoggerFactory.getLogger(this::class.java)
    }
}
