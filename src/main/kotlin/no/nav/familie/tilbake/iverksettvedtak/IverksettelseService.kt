package no.nav.familie.tilbake.iverksettvedtak

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingsvedtakService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Iverksettingsstatus
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.iverksettvedtak.domain.KodeResultat
import no.nav.familie.tilbake.iverksettvedtak.domain.Tilbakekrevingsbeløp
import no.nav.familie.tilbake.iverksettvedtak.domain.Tilbakekrevingsperiode
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.IverksettService
import no.nav.tilbakekreving.entities.AktørEntity
import no.nav.tilbakekreving.entities.AktørType
import no.nav.tilbakekreving.integrasjoner.oppdrag.OppdragRestClient
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.KodeAksjonDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.PosteringDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.TilbakekrevingsvedtakRequestDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.VedtakPeriodeDto
import no.nav.tilbakekreving.vedtak.IverksattVedtak
import no.nav.tilbakekreving.vedtak.IverksettRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
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
            val kvittering = oppdragRestClient.iverksettVedtak(request)
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
        beregnetPerioder: List<Tilbakekrevingsperiode>,
        logContext: SecureLog.Context,
    ): TilbakekrevingsvedtakRequestDto {
        return TilbakekrevingsvedtakRequestDto(
            kodeAksjon = KodeAksjonDto.FATTE_VEDTAK,
            vedtakId = kravgrunnlag.vedtakId,
            vedtaksDato = kravgrunnlag.fagsystemVedtaksdato ?: LocalDate.now(),
            kodeHjemmel = "22-15", // fast verdi
            renterBeregnes = beregnetPerioder.any { it.renter > BigDecimal.ZERO },
            enhetAnsvarlig = kravgrunnlag.ansvarligEnhet,
            kontrollfelt = kravgrunnlag.kontrollfelt,
            saksbehandlerId = ansvarligSaksbehandler,
            perioder = lagVedtaksperiode(beregnetPerioder, logContext),
            datoTilleggsfrist = null,
        )
    }

    private fun lagVedtaksperiode(
        beregnetPerioder: List<Tilbakekrevingsperiode>,
        logContext: SecureLog.Context,
    ) = beregnetPerioder.map { periode ->
        VedtakPeriodeDto(
            periodeFom = periode.periode.fom.atDay(1),
            periodeTom = periode.periode.tom.atEndOfMonth(),
            renterPeriodeBeregnes = periode.renter > BigDecimal.ZERO,
            belopRenter = periode.renter,
            posteringer = lagPosteringer(periode.beløp, logContext),
        )
    }

    private fun lagPosteringer(
        beregnetBeløper: List<Tilbakekrevingsbeløp>,
        logContext: SecureLog.Context,
    ) = beregnetBeløper.map {
        when (it.klassetype) {
            Klassetype.YTEL -> PosteringDto(
                kodeKlasse = it.klassekode.tilKlassekodeNavn(),
                belopOpprinneligUtbetalt = it.utbetaltBeløp,
                belopNy = it.nyttBeløp,
                belopTilbakekreves = it.tilbakekrevesBeløp,
                belopUinnkrevd = it.uinnkrevdBeløp,
                belopSkatt = it.skattBeløp,
                kodeResultat = utledKodeResultat(it, logContext),
                kodeAarsak = "ANNET", // fast verdi
                kodeSkyld = "IKKE_FORDELT", // fast verdi
            )

            else ->
                PosteringDto(
                    kodeKlasse = it.klassekode.tilKlassekodeNavn(),
                    belopOpprinneligUtbetalt = it.utbetaltBeløp,
                    belopNy = it.nyttBeløp,
                    belopTilbakekreves = it.tilbakekrevesBeløp,
                    belopUinnkrevd = it.uinnkrevdBeløp,
                    belopSkatt = it.skattBeløp,
                    kodeResultat = "",
                    kodeAarsak = "", // fast verdi
                    kodeSkyld = "", // fast verdi
                )
        }
    }

    private fun utledKodeResultat(
        tilbakekrevingsbeløp: Tilbakekrevingsbeløp,
        logContext: SecureLog.Context,
    ): String =
        if (harSattDelvisTilbakekrevingMenKreverTilbakeFulltBeløp(tilbakekrevingsbeløp)) {
            SecureLog.medContext(logContext) {
                warn(
                    """Fant tilbakekrevingsperiode med delvis tilbakekreving hvor vi krever tilbake hele beløpet.
                | Økonomi krever trolig at vi setter full tilbakekreving. 
                | Dersom kjøringen feiler mot økonomi med feilmelding: Innkrevd beløp = feilutbetalt ved delvis tilbakekreving.
                | Vurder å skru på featuretoggle familie-tilbake-overstyr-delvis-tilbakekreving og rekjør.
                | Tilbakekrevingsbeløp=$tilbakekrevingsbeløp """.trimMargin(),
                )
            }
            KodeResultat.DELVIS_TILBAKEKREVING.kode
        } else {
            tilbakekrevingsbeløp.kodeResultat.kode
        }

    private fun harSattDelvisTilbakekrevingMenKreverTilbakeFulltBeløp(tilbakekrevingsbeløp: Tilbakekrevingsbeløp) = tilbakekrevingsbeløp.kodeResultat == KodeResultat.DELVIS_TILBAKEKREVING && tilbakekrevingsbeløp.uinnkrevdBeløp == BigDecimal.ZERO
}
