package no.nav.familie.tilbake.iverksettvedtak

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingsvedtakService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Iverksettingsstatus
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.foreldelse.ForeldelseService
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.IverksettService
import no.nav.tilbakekreving.entities.AktørEntity
import no.nav.tilbakekreving.entities.AktørType
import no.nav.tilbakekreving.integrasjoner.feil.UnexpectedResponseException
import no.nav.tilbakekreving.integrasjoner.oppdrag.OppdragRestClient
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.KodeAksjonDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.TilbakekrevingsvedtakRequestDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.VedtakPeriodeDto
import no.nav.tilbakekreving.vedtak.IverksattVedtak
import no.nav.tilbakekreving.vedtak.IverksettRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class IverksettelseService(
    private val behandlingRepository: BehandlingRepository,
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val tilbakekrevingsvedtakBeregningService: TilbakekrevingsvedtakBeregningService,
    private val behandlingVedtakService: BehandlingsvedtakService,
    private val oppdragRestClient: OppdragRestClient,
    private val logService: LogService,
    private val fagsakRepository: FagsakRepository,
    private val iverksettRepository: IverksettRepository,
    private val foreldelseService: ForeldelseService,
) {
    private val log = TracedLogger.getLogger<IverksettelseService>()

    @Transactional
    fun sendIverksettVedtak(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val logContext = logService.contextFraBehandling(behandling.id)

        if (behandling.erAvsluttet) {
            log.medContext(logContext) {
                info("Behandling med id ${behandling.id} er iverksatt mot økonomi - kan ikke iverksette flere ganger")
            }
        } else {
            val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
            val beregnetPerioder = tilbakekrevingsvedtakBeregningService.beregnVedtaksperioder(behandlingId, kravgrunnlag)

            // Send request til økonomi
            val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
            val request = lagIveksettelseRequest(
                ansvarligSaksbehandler = behandling.ansvarligSaksbehandler,
                kravgrunnlag = kravgrunnlag,
                beregnetPerioder = beregnetPerioder,
                logContext = logContext,
            )
            log.medContext(logContext) {
                info("Iverksetter vedtak med REST-endepunkt (gammel modell)")
            }
            val kvittering = try {
                oppdragRestClient.iverksettVedtak(request)
            } catch (e: UnexpectedResponseException) {
                log.medContext(logContext) {
                    warn("Feilet under iverksettelse av vedtak, status={}, response {}", e.statusCode, e.response, e)
                }
                throw e
            }
            IverksettService.validerIverksettelse(kvittering, logContext)
            iverksettRepository.lagreIverksattVedtak(
                IverksattVedtak(
                    id = UUID.randomUUID(),
                    behandlingId = behandlingId,
                    nyModell = false,
                    vedtakId = kravgrunnlag.vedtakId,
                    aktør = when (val institusjon = fagsak.institusjon) {
                        null -> AktørEntity(AktørType.Person, fagsak.bruker.ident)
                        else -> AktørEntity(AktørType.Organisasjon, institusjon.organisasjonsnummer)
                    },
                    ytelsestypeKode = fagsak.ytelsestype.kode,
                    kvittering = String.format("%02d", kvittering.status),
                    perioder = IverksattVedtak.IverksattPeriode.fra(request),
                    vedtaksdato = LocalDate.now(),
                    behandlingstype = behandling.type,
                ),
            )
            behandlingVedtakService.oppdaterBehandlingsvedtak(behandlingId, Iverksettingsstatus.IVERKSATT)
        }
    }

    private fun lagIveksettelseRequest(
        ansvarligSaksbehandler: String,
        kravgrunnlag: Kravgrunnlag431,
        beregnetPerioder: List<VedtakPeriodeDto>,
        logContext: SecureLog.Context,
    ): TilbakekrevingsvedtakRequestDto {
        return TilbakekrevingsvedtakRequestDto(
            kodeAksjon = KodeAksjonDto.FATTE_VEDTAK,
            vedtakId = kravgrunnlag.vedtakId,
            vedtaksDato = kravgrunnlag.fagsystemVedtaksdato ?: LocalDate.now(),
            kodeHjemmel = "22-15", // fast verdi
            // Feltet bestemmer om Økonomi skal beregne renter for oss. Settes til false siden vi beregner det selv.
            renterBeregnes = false,
            enhetAnsvarlig = kravgrunnlag.ansvarligEnhet,
            kontrollfelt = kravgrunnlag.kontrollfelt,
            saksbehandlerId = ansvarligSaksbehandler,
            perioder = beregnetPerioder,
            datoTilleggsfrist = foreldelseService.hentOppdagelsesdatoForTilleggsfrist(kravgrunnlag.behandlingId, logContext),
        )
    }
}
