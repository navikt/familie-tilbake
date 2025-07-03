package no.nav.familie.tilbake.iverksettvedtak

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingsvedtakService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Iverksettingsstatus
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.integration.økonomi.OppdragClient
import no.nav.familie.tilbake.iverksettvedtak.domain.KodeResultat
import no.nav.familie.tilbake.iverksettvedtak.domain.Tilbakekrevingsbeløp
import no.nav.familie.tilbake.iverksettvedtak.domain.Tilbakekrevingsperiode
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.KodeAksjon
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakRequest
import no.nav.tilbakekreving.entities.AktørEntity
import no.nav.tilbakekreving.entities.AktørType
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsbelopDto
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsperiodeDto
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsvedtakDto
import no.nav.tilbakekreving.typer.v1.MmelDto
import no.nav.tilbakekreving.typer.v1.PeriodeDto
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
    private val fagsakRepository: FagsakRepository,
    private val iverksettRepository: IverksettRepository,
    private val tilbakekrevingsvedtakBeregningService: TilbakekrevingsvedtakBeregningService,
    private val behandlingVedtakService: BehandlingsvedtakService,
    private val oppdragClient: OppdragClient,
    private val logService: LogService,
) {
    private val log = TracedLogger.getLogger<IverksettelseService>()

    @Transactional
    fun sendIverksettVedtak(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val logContext = logService.contextFraBehandling(behandling.id)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)

        if (behandling.erAvsluttet) {
            log.medContext(logContext) {
                info("Behandling med id ${behandling.id} er iverksatt mot økonomi - kan ikke iverksette flere ganger")
            }
        } else {
            val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
            val beregnetPerioder = tilbakekrevingsvedtakBeregningService.beregnVedtaksperioder(behandlingId, kravgrunnlag)
            val request = lagIveksettelseRequest(
                ansvarligSaksbehandler = behandling.ansvarligSaksbehandler,
                kravgrunnlag = kravgrunnlag,
                beregnetPerioder = beregnetPerioder,
                logContext = logContext,
            )
            val requestXml = TilbakekrevingsvedtakMarshaller.marshall(behandlingId, request, logContext)
            SecureLog.medContext(logContext) {
                info("Sender tilbakekrevingsvedtak til økonomi for behandlingId={} request={}", behandlingId.toString(), requestXml)
            }

            val mmel: MmelDto = oppdragClient.iverksettVedtak(behandlingId, request, logContext).mmel

            val behandlingId = behandlingId
            val vedtakId = request.tilbakekrevingsvedtak.vedtakId
            val aktør = if (fagsak.institusjon != null) AktørEntity(AktørType.Organisasjon, fagsak.institusjon.organisasjonsnummer) else AktørEntity(AktørType.Person, fagsak.bruker.ident)
            val ytelsestype = fagsak.ytelsestype
            val kvittering2 = mmel.alvorlighetsgrad
            val tilbakekrevingsvedtak = request.tilbakekrevingsvedtak
            val behandlingstype = behandling.type

            val iverksattVedtak = IverksattVedtak(
                behandlingId = behandlingId,
                vedtakId = vedtakId,
                aktør = aktør,
                ytelsestypeKode = ytelsestype.kode,
                kvittering = kvittering2,
                tilbakekrevingsvedtak = tilbakekrevingsvedtak,
                behandlingstype = behandlingstype,
            )

            iverksettRepository.lagreIverksattVedtak(iverksattVedtak)
            behandlingVedtakService.oppdaterBehandlingsvedtak(behandlingId, Iverksettingsstatus.IVERKSATT)
        }
    }

    private fun lagIveksettelseRequest(
        ansvarligSaksbehandler: String,
        kravgrunnlag: Kravgrunnlag431,
        beregnetPerioder: List<Tilbakekrevingsperiode>,
        logContext: SecureLog.Context,
    ): TilbakekrevingsvedtakRequest {
        val request = TilbakekrevingsvedtakRequest()
        val vedtak = TilbakekrevingsvedtakDto()
        vedtak.apply {
            vedtakId = kravgrunnlag.vedtakId
            kodeAksjon = KodeAksjon.FATTE_VEDTAK.kode
            kodeHjemmel = "22-15" // fast verdi
            datoVedtakFagsystem = kravgrunnlag.fagsystemVedtaksdato ?: LocalDate.now()
            enhetAnsvarlig = kravgrunnlag.ansvarligEnhet
            kontrollfelt = kravgrunnlag.kontrollfelt
            saksbehId = ansvarligSaksbehandler
            tilbakekrevingsperiode.addAll(lagVedtaksperiode(beregnetPerioder, logContext))
        }
        return request.apply { tilbakekrevingsvedtak = vedtak }
    }

    private fun lagVedtaksperiode(
        beregnetPerioder: List<Tilbakekrevingsperiode>,
        logContext: SecureLog.Context,
    ): List<TilbakekrevingsperiodeDto> =
        beregnetPerioder.map {
            val tilbakekrevingsperiode = TilbakekrevingsperiodeDto()
            tilbakekrevingsperiode.apply {
                val periode = PeriodeDto()
                periode.fom = it.periode.fom.atDay(1)
                periode.tom = it.periode.tom.atEndOfMonth()
                this.periode = periode
                belopRenter = it.renter
                tilbakekrevingsbelop.addAll(lagVedtaksbeløp(it.beløp, logContext))
            }
        }

    private fun lagVedtaksbeløp(
        beregnetBeløper: List<Tilbakekrevingsbeløp>,
        logContext: SecureLog.Context,
    ): List<TilbakekrevingsbelopDto> =
        beregnetBeløper.map {
            val tilbakekrevingsbeløp = TilbakekrevingsbelopDto()
            tilbakekrevingsbeløp.apply {
                kodeKlasse = it.klassekode.tilKlassekodeNavn()
                belopNy = it.nyttBeløp
                belopOpprUtbet = it.utbetaltBeløp
                belopTilbakekreves = it.tilbakekrevesBeløp
                belopUinnkrevd = it.uinnkrevdBeløp
                belopSkatt = it.skattBeløp
                if (Klassetype.YTEL == it.klassetype) {
                    kodeResultat = utledKodeResultat(it, logContext)
                    kodeAarsak = "ANNET" // fast verdi
                    kodeSkyld = "IKKE_FORDELT" // fast verdi
                }
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
