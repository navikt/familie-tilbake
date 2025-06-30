package no.nav.tilbakekreving

import no.nav.familie.tilbake.integration.økonomi.OppdragClient
import no.nav.familie.tilbake.iverksettvedtak.domain.KodeResultat
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import no.nav.familie.tilbake.kravgrunnlag.domain.KodeAksjon
import no.nav.familie.tilbake.log.SecureLog
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakRequest
import no.nav.okonomi.tilbakekrevingservice.TilbakekrevingsvedtakResponse
import no.nav.tilbakekreving.behov.IverksettelseBehov
import no.nav.tilbakekreving.beregning.delperiode.Delperiode
import no.nav.tilbakekreving.beregning.delperiode.Foreldet
import no.nav.tilbakekreving.beregning.isZero
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagBufferRepository
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagBelopDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagPeriodeDto
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsbelopDto
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsperiodeDto
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsvedtakDto
import no.nav.tilbakekreving.typer.v1.PeriodeDto
import no.nav.tilbakekreving.typer.v1.TypeKlasseDto
import no.nav.tilbakekreving.vedtak.IverksattVedtak
import no.nav.tilbakekreving.vedtak.IverksettRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class IverksettService(
    private val kravgrunnlagBufferRepository: KravgrunnlagBufferRepository,
    private val iverksettRepository: IverksettRepository,
    private val oppdragClient: OppdragClient,
) {
    fun iverksett(
        iverksettelseBehov: IverksettelseBehov,
        logContext: SecureLog.Context,
    ) {
        val behandlingId = iverksettelseBehov.behandlingId
        val entity = kravgrunnlagBufferRepository.hentKravgrunnlag(iverksettelseBehov.kravgrunnlagId)
            ?: error("Fant ikke kravgrunnlag for $${iverksettelseBehov.kravgrunnlagId}")
        val kravgrunnlag = KravgrunnlagUtil.unmarshalKravgrunnlag(entity.kravgrunnlag)

        val request = lagIverksettelseRequest(
            ansvarligSaksbehandler = iverksettelseBehov.ansvarligSaksbehandler,
            kravgrunnlag = kravgrunnlag,
            beregnetPerioder = iverksettelseBehov.delperioder,
        )

        val kvittering = oppdragClient.iverksettVedtak(
            behandlingId = behandlingId,
            tilbakekrevingsvedtakRequest = request,
            logContext = logContext,
        )

        lagreIverksattVedtak(iverksettelseBehov, kravgrunnlag, request, kvittering)
    }

    fun lagreIverksattVedtak(
        iverksettelseBehov: IverksettelseBehov,
        kravgrunnlag: DetaljertKravgrunnlagDto,
        request: TilbakekrevingsvedtakRequest,
        kvittering: TilbakekrevingsvedtakResponse,
    ) {
        val iverksattVedtak = IverksattVedtak(
            behandlingId = iverksettelseBehov.behandlingId,
            vedtakId = kravgrunnlag.vedtakId,
            aktør = iverksettelseBehov.aktør.tilEntity(),
            opprettetTid = kravgrunnlag.datoVedtakFagsystem,
            ytelsestype = iverksettelseBehov.ytelsestype,
            kvittering = kvittering.mmel.alvorlighetsgrad,
            tilbakekrevingsperioder = request.tilbakekrevingsvedtak.tilbakekrevingsperiode,
            behandlingstype = iverksettelseBehov.behandlingstype,
        )
        iverksettRepository.lagreIverksattVedtak(iverksattVedtak)
    }

    private fun lagIverksettelseRequest(
        ansvarligSaksbehandler: String,
        kravgrunnlag: DetaljertKravgrunnlagDto,
        beregnetPerioder: List<Delperiode<out Delperiode.Beløp>>,
    ): TilbakekrevingsvedtakRequest {
        val request = TilbakekrevingsvedtakRequest()
        val vedtak = TilbakekrevingsvedtakDto()
        vedtak.apply {
            vedtakId = kravgrunnlag.vedtakId
            kodeAksjon = KodeAksjon.FATTE_VEDTAK.kode
            kodeHjemmel = "22-15" // fast verdi
            datoVedtakFagsystem = kravgrunnlag.datoVedtakFagsystem ?: LocalDate.now()
            enhetAnsvarlig = kravgrunnlag.enhetAnsvarlig
            kontrollfelt = kravgrunnlag.kontrollfelt
            saksbehId = ansvarligSaksbehandler
            tilbakekrevingsperiode.addAll(lagVedtaksperiode(beregnetPerioder, kravgrunnlag.tilbakekrevingsPeriode))
        }
        return request.apply { tilbakekrevingsvedtak = vedtak }
    }

    private fun lagVedtaksperiode(
        beregnetPerioder: List<Delperiode<out Delperiode.Beløp>>,
        kravgrunnlagPeriode: List<DetaljertKravgrunnlagPeriodeDto>,
    ): List<TilbakekrevingsperiodeDto> =
        kravgrunnlagPeriode.map { kgPeriode ->
            val tilbakekrevingsperiode = TilbakekrevingsperiodeDto()
            val beregnetPeriode = beregnetPerioder.single { it.periode.snitt(kgPeriode.periode.fom til kgPeriode.periode.tom) != null }
            tilbakekrevingsperiode.apply {
                val periode = PeriodeDto()
                periode.fom = beregnetPeriode.periode.fom
                periode.tom = beregnetPeriode.periode.tom
                this.periode = periode
                belopRenter = beregnetPeriode.renter()
                tilbakekrevingsbelop.addAll(lagVedtaksbeløp(beregnetPeriode, kgPeriode.tilbakekrevingsBelop))
            }
        }

    private fun lagVedtaksbeløp(
        delperiode: Delperiode<out Delperiode.Beløp>,
        kravgrunnlagBeløp: List<DetaljertKravgrunnlagBelopDto>,
    ): List<TilbakekrevingsbelopDto> =
        kravgrunnlagBeløp.mapNotNull {
            when (it.typeKlasse) {
                TypeKlasseDto.YTEL -> TilbakekrevingsbelopDto().apply {
                    val beløp = delperiode.beløpForKlassekode(it.kodeKlasse)
                    kodeKlasse = it.kodeKlasse
                    belopNy = it.belopNy.setScale(0, RoundingMode.HALF_UP)
                    belopOpprUtbet = beløp.utbetaltYtelsesbeløp()
                    belopTilbakekreves = beløp.tilbakekrevesBrutto()
                    belopUinnkrevd = it.belopTilbakekreves
                        .subtract(beløp.tilbakekrevesBrutto())
                        .setScale(0, RoundingMode.HALF_UP)
                    belopSkatt = beløp.skatt()
                    kodeResultat = utledKodeResulat(delperiode).kode
                    kodeAarsak = "ANNET" // fast verdi
                    kodeSkyld = "IKKE_FORDELT" // fast verdi
                }
                TypeKlasseDto.FEIL -> TilbakekrevingsbelopDto().apply {
                    kodeKlasse = it.kodeKlasse
                    belopNy = it.belopNy
                    belopOpprUtbet = BigDecimal.ZERO
                    belopTilbakekreves = BigDecimal.ZERO
                    belopUinnkrevd = BigDecimal.ZERO
                    belopSkatt = BigDecimal.ZERO
                }
                else -> null
            }
        }

    private fun utledKodeResulat(beregnetPeriode: Delperiode<out Delperiode.Beløp>): KodeResultat = when {
        beregnetPeriode is Foreldet.ForeldetPeriode -> KodeResultat.FORELDET
        beregnetPeriode.beløp().sumOf { it.tilbakekrevesBrutto() }.isZero() -> KodeResultat.INGEN_TILBAKEKREVING
        beregnetPeriode.feilutbetaltBeløp() == beregnetPeriode.beløp().sumOf { it.tilbakekrevesBrutto() } -> KodeResultat.FULL_TILBAKEKREVING
        else -> KodeResultat.DELVIS_TILBAKEKREVING
    }
}
