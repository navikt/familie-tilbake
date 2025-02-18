package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.integration.økonomi.OppdragClient
import no.nav.familie.tilbake.kravgrunnlag.domain.KodeAksjon
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.okonomi.tilbakekrevingservice.KravgrunnlagHentDetaljRequest
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.HentKravgrunnlagDetaljDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.time.LocalDateTime
import java.util.UUID

@Service
class HentKravgrunnlagService(
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val oppdragClient: OppdragClient,
    private val historikkService: HistorikkService,
) {
    private val log = TracedLogger.getLogger<HentKravgrunnlagService>()

    fun hentKravgrunnlagFraØkonomi(
        kravgrunnlagId: BigInteger,
        kodeAksjon: KodeAksjon,
        logContext: SecureLog.Context,
    ): DetaljertKravgrunnlagDto {
        log.medContext(logContext) {
            info("Henter kravgrunnlag for kravgrunnlagId=$kravgrunnlagId for kodeAksjon=$kodeAksjon")
        }
        return oppdragClient.hentKravgrunnlag(kravgrunnlagId, lagRequest(kravgrunnlagId, kodeAksjon), logContext)
    }

    fun hentTilbakekrevingskravgrunnlag(behandlingId: UUID): Kravgrunnlag431 = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)

    @Transactional
    fun lagreHentetKravgrunnlag(
        behandlingId: UUID,
        kravgrunnlag: DetaljertKravgrunnlagDto,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Lagrer hentet kravgrunnlag for behandling $behandlingId")
        }
        val kravgrunnlag431 = KravgrunnlagMapper.tilKravgrunnlag431(kravgrunnlag, behandlingId)
        kravgrunnlagRepository.insert(kravgrunnlag431)
    }

    @Transactional
    fun opprettHistorikkinnslag(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info(
                "Oppretter historikkinnslag ${TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_HENT} " +
                    "for behandling $behandlingId",
            )
        }
        historikkService.lagHistorikkinnslag(
            behandlingId = behandlingId,
            historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.KRAVGRUNNLAG_HENT,
            aktør = Aktør.Vedtaksløsning,
            opprettetTidspunkt = LocalDateTime.now(),
        )
    }

    private fun lagRequest(
        kravgrunnlagId: BigInteger,
        kodeAksjon: KodeAksjon,
    ): KravgrunnlagHentDetaljRequest {
        val hentkravgrunnlag = HentKravgrunnlagDetaljDto()
        hentkravgrunnlag.kravgrunnlagId = kravgrunnlagId
        hentkravgrunnlag.kodeAksjon = kodeAksjon.kode
        hentkravgrunnlag.enhetAnsvarlig = "8020" // fast verdi
        hentkravgrunnlag.saksbehId = "K231B433" // fast verdi

        val request = KravgrunnlagHentDetaljRequest()
        request.hentkravgrunnlag = hentkravgrunnlag

        return request
    }
}
