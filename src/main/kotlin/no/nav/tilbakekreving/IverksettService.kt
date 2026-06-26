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
import no.nav.tilbakekreving.config.FeatureService
import no.nav.tilbakekreving.integrasjoner.oppdrag.OppdragRestClient
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.KodeAksjonDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.PosteringDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.TilbakekrevingsvedtakRequestDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.TilbakekrevingsvedtakResponseDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.VedtakPeriodeDto
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
import java.util.UUID

@Service
class IverksettService(
    private val kravgrunnlagBufferRepository: KravgrunnlagBufferRepository,
    private val iverksettRepository: IverksettRepository,
    private val oppdragClient: OppdragClient,
    private val oppdragRestClient: OppdragRestClient,
    private val featureService: FeatureService,
) {
    fun iverksett(
        iverksettelseBehov: IverksettelseBehov,
        logContext: SecureLog.Context,
    ): IverksattVedtak {
        val behandlingId = iverksettelseBehov.behandlingId
        val kravgrunnlagListe = kravgrunnlagBufferRepository.hentKravgrunnlag(iverksettelseBehov.kravgrunnlagId)
            .ifEmpty { error("Fant ikke kravgrunnlag for ${iverksettelseBehov.kravgrunnlagId}") }
            .map { KravgrunnlagUtil.unmarshalKravgrunnlag(it.kravgrunnlag) }

        val kravgrunnlag = kravgrunnlagListe.singleOrNull { it.kontrollfelt == iverksettelseBehov.kravgrunnlagInfo.kontrollfelt }
            ?: error("Kunne ikke finne kravgrunnlag med riktig kontrollfelt")

        return if (featureService.modellFeatures[Toggle.OppdragRestClient]) {
            val request = lagNyIverksettelseRequest(
                ansvarligSaksbehandler = iverksettelseBehov.ansvarligSaksbehandler,
                kravgrunnlag = kravgrunnlag,
                beregnetPerioder = iverksettelseBehov.delperioder,
            )
            val kvittering = oppdragRestClient.iverksettVedtak(request)
            lagreNyIverksattVedtak(iverksettelseBehov, request, kvittering)
        } else {
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
            lagreIverksattVedtak(iverksettelseBehov, request, kvittering)
        }
    }

    fun lagreIverksattVedtak(
        iverksettelseBehov: IverksettelseBehov,
        request: TilbakekrevingsvedtakRequest,
        kvittering: TilbakekrevingsvedtakResponse,
    ): IverksattVedtak {
        val iverksattVedtak = IverksattVedtak(
            id = UUID.randomUUID(),
            behandlingId = iverksettelseBehov.behandlingId,
            vedtakId = request.tilbakekrevingsvedtak.vedtakId,
            nyModell = true,
            aktør = iverksettelseBehov.aktør.tilEntity(),
            ytelsestypeKode = iverksettelseBehov.ytelse.tilYtelsestype().kode,
            kvittering = kvittering.mmel.alvorlighetsgrad,
            perioder = IverksattVedtak.IverksattPeriode.fra(request),
            behandlingstype = iverksettelseBehov.behandlingstype,
            vedtaksdato = iverksettelseBehov.vedtaksdato,
        )
        iverksettRepository.lagreIverksattVedtak(iverksattVedtak)
        return iverksattVedtak
    }

    fun lagreNyIverksattVedtak(
        iverksettelseBehov: IverksettelseBehov,
        request: TilbakekrevingsvedtakRequestDto,
        kvittering: TilbakekrevingsvedtakResponseDto,
    ): IverksattVedtak {
        val iverksattVedtak = IverksattVedtak(
            id = UUID.randomUUID(),
            behandlingId = iverksettelseBehov.behandlingId,
            nyModell = true,
            vedtakId = request.vedtakId,
            aktør = iverksettelseBehov.aktør.tilEntity(),
            ytelsestypeKode = iverksettelseBehov.ytelse.tilYtelsestype().kode,
            kvittering = String.format("%02d", kvittering.status),
            perioder = IverksattVedtak.IverksattPeriode.fra(request),
            behandlingstype = iverksettelseBehov.behandlingstype,
            vedtaksdato = iverksettelseBehov.vedtaksdato,
        )
        iverksettRepository.lagreIverksattVedtak(iverksattVedtak)
        return iverksattVedtak
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

    private fun lagNyIverksettelseRequest(
        ansvarligSaksbehandler: String,
        kravgrunnlag: DetaljertKravgrunnlagDto,
        beregnetPerioder: List<Delperiode<out Delperiode.Beløp>>,
    ): TilbakekrevingsvedtakRequestDto {
        return TilbakekrevingsvedtakRequestDto(
            kodeAksjon = KodeAksjonDto.FATTE_VEDTAK,
            vedtakId = kravgrunnlag.vedtakId,
            vedtaksDato = kravgrunnlag.datoVedtakFagsystem ?: LocalDate.now(),
            kodeHjemmel = "22-15",
            renterBeregnes = beregnetPerioder.any { it.harRenter() },
            enhetAnsvarlig = kravgrunnlag.enhetAnsvarlig,
            kontrollfelt = kravgrunnlag.kontrollfelt,
            saksbehandlerId = ansvarligSaksbehandler,
            perioder = lagNyVedtaksperiode(beregnetPerioder, kravgrunnlag.tilbakekrevingsPeriode),
            datoTilleggsfrist = null,
        )
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

    private fun lagNyVedtaksperiode(
        beregnetPerioder: List<Delperiode<out Delperiode.Beløp>>,
        kravgrunnlagPeriode: List<DetaljertKravgrunnlagPeriodeDto>,
    ): List<VedtakPeriodeDto> = kravgrunnlagPeriode.map { kgPeriode ->
        val beregnetPeriode = beregnetPerioder.single { it.periode.snitt(kgPeriode.periode.fom til kgPeriode.periode.tom) != null }
        VedtakPeriodeDto(
            periodeFom = kgPeriode.periode.fom,
            periodeTom = kgPeriode.periode.tom,
            renterPeriodeBeregnes = beregnetPeriode.harRenter(),
            belopRenter = beregnetPeriode.renter(),
            posteringer = lagPosteringer(beregnetPeriode, kgPeriode.tilbakekrevingsBelop),
        )
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

    private fun lagPosteringer(
        delperiode: Delperiode<out Delperiode.Beløp>,
        kravgrunnlagBeløp: List<DetaljertKravgrunnlagBelopDto>,
    ): List<PosteringDto> = kravgrunnlagBeløp.mapNotNull {
        val beløp = delperiode.beløpForKlassekode(it.kodeKlasse)
        when (it.typeKlasse) {
            TypeKlasseDto.YTEL -> PosteringDto(
                kodeKlasse = it.kodeKlasse,
                belopNy = it.belopNy.setScale(0, RoundingMode.HALF_UP),
                belopOpprinneligUtbetalt = beløp.utbetaltYtelsesbeløp(),
                belopTilbakekreves = beløp.tilbakekrevesBrutto(),
                belopUinnkrevd = it.belopTilbakekreves
                    .subtract(beløp.tilbakekrevesBrutto())
                    .setScale(0, RoundingMode.HALF_UP),
                belopSkatt = beløp.skatt(),
                kodeResultat = utledKodeResulat(delperiode).kode,
                kodeAarsak = "ANNET", // fast verdi
                kodeSkyld = "IKKE_FORDELT", // fast verdi
            )

            TypeKlasseDto.FEIL -> PosteringDto(
                kodeKlasse = it.kodeKlasse,
                belopNy = it.belopNy,
                belopOpprinneligUtbetalt = BigDecimal.ZERO,
                belopTilbakekreves = BigDecimal.ZERO,
                belopUinnkrevd = BigDecimal.ZERO,
                belopSkatt = BigDecimal.ZERO,
                kodeResultat = "",
                kodeAarsak = "",
                kodeSkyld = "",
            )

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
