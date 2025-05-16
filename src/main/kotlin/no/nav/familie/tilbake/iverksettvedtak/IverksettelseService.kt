package no.nav.familie.tilbake.iverksettvedtak

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingsvedtakService
import no.nav.familie.tilbake.behandling.domain.Iverksettingsstatus
import no.nav.familie.tilbake.beregning.TilbakekrevingsberegningService
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.integration.økonomi.OppdragClient
import no.nav.familie.tilbake.iverksettvedtak.domain.KodeResultat
import no.nav.familie.tilbake.iverksettvedtak.domain.Tilbakekrevingsbeløp
import no.nav.familie.tilbake.iverksettvedtak.domain.Tilbakekrevingsperiode
import no.nav.familie.tilbake.iverksettvedtak.domain.ØkonomiXmlSendt
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.KodeAksjon
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakRequest
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsbelopDto
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsperiodeDto
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsvedtakDto
import no.nav.tilbakekreving.typer.v1.PeriodeDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Service
class IverksettelseService(
    private val behandlingRepository: BehandlingRepository,
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val økonomiXmlSendtRepository: ØkonomiXmlSendtRepository,
    private val tilbakekrevingsvedtakBeregningService: TilbakekrevingsvedtakBeregningService,
    private val beregningService: TilbakekrevingsberegningService,
    private val behandlingVedtakService: BehandlingsvedtakService,
    private val oppdragClient: OppdragClient,
    private val logService: LogService,
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

            // Send request til økonomi
            val kvittering = objectMapper.writeValueAsString(oppdragClient.iverksettVedtak(behandlingId, request, logContext).mmel)
            lagreIverksettelsesvedtakRequest(behandlingId, requestXml, kvittering)
            behandlingVedtakService.oppdaterBehandlingsvedtak(behandlingId, Iverksettingsstatus.IVERKSATT)
        }
    }

    fun lagreIverksettelsesvedtakRequest(
        behandlingId: UUID,
        requestXml: String,
        kvittering: String?,
    ): ØkonomiXmlSendt = økonomiXmlSendtRepository.insert(ØkonomiXmlSendt(behandlingId = behandlingId, melding = requestXml, kvittering = kvittering))

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
