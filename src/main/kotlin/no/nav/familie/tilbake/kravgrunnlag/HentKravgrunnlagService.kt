package no.nav.familie.tilbake.kravgrunnlag

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.familie.tilbake.common.exceptionhandler.IntegrasjonException
import no.nav.familie.tilbake.common.exceptionhandler.SperretKravgrunnlagFeil
import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagMapper.tilDetaljertKravgrunnlagDto
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.integrasjoner.oppdrag.OppdragRestClient
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.HentKravgrunnlagDetaljerResponseDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.KodeAksjonDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.time.LocalDateTime
import java.util.UUID

@Service
class HentKravgrunnlagService(
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val historikkService: HistorikkService,
    private val oppdragRestClient: OppdragRestClient,
) {
    private val log = TracedLogger.getLogger<HentKravgrunnlagService>()

    fun hentKravgrunnlagFraØkonomi(
        kravgrunnlagId: BigInteger,
        kodeAksjon: KodeAksjonDto,
        logContext: SecureLog.Context,
    ): DetaljertKravgrunnlagDto {
        log.medContext(logContext) {
            info(
                "Henter kravgrunnlag for kravgrunnlagId={} for kodeAksjon={}, rest=true",
                kravgrunnlagId,
                kodeAksjon,
            )
        }
        val response = oppdragRestClient.hentKravgrunnlag(kravgrunnlagId, kodeAksjon)
        validerHentKravgrunnlagRespons(response, kravgrunnlagId, logContext)
        return response.kravgrunnlag.tilDetaljertKravgrunnlagDto()
    }

    private fun validerHentKravgrunnlagRespons(
        response: HentKravgrunnlagDetaljerResponseDto,
        kravgrunnlagId: BigInteger,
        logContext: SecureLog.Context,
    ) {
        if (!OppdragRestClient.erResponseOk(response.status) || erKravgrunnlagIkkeFinnes(response)) {
            SecureLog.medContext(logContext) {
                warn("Mottok ugyldig kravgrunnlag. Mangler feltet `detaljertKravgrunnlag`. {}. {}", keyValue("kravgrunnlagId", kravgrunnlagId), objectMapper.writeValueAsString(response))
            }
            log.medContext(logContext) {
                error(
                    "Fikk feil respons:${response.status} fra økonomi ved henting av kravgrunnlag " +
                        "for kravgrunnlagId=$kravgrunnlagId.",
                )
            }
            throw IntegrasjonException(
                msg =
                    "Fikk feil respons:${response.status} fra økonomi " +
                        "ved henting av kravgrunnlag for kravgrunnlagId=$kravgrunnlagId.",
                logContext = logContext,
            )
        } else if (erKravgrunnlagSperret(response)) {
            log.medContext(logContext) {
                warn("Hentet kravgrunnlag for kravgrunnlagId=$kravgrunnlagId er sperret")
            }
            throw SperretKravgrunnlagFeil(
                melding = "Hentet kravgrunnlag for kravgrunnlagId=$kravgrunnlagId er sperret",
                logContext = logContext,
            )
        }
    }

    private fun erKravgrunnlagSperret(response: HentKravgrunnlagDetaljerResponseDto): Boolean = KODE_MELDING_SPERRET_KRAVGRUNNLAG == response.melding

    private fun erKravgrunnlagIkkeFinnes(response: HentKravgrunnlagDetaljerResponseDto): Boolean = KODE_MELDING_KRAVGRUNNLAG_IKKE_FINNES == response.melding

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

        try {
            kravgrunnlagRepository.insert(kravgrunnlag431)
        } catch (e: Exception) {
            log.medContext(logContext) {
                error("Feil ved insert av kravgrunnlag", e)
            }
        }
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

    companion object {
        const val KODE_MELDING_SPERRET_KRAVGRUNNLAG = "B420012I"
        const val KODE_MELDING_KRAVGRUNNLAG_IKKE_FINNES = "B420010I"
    }
}
