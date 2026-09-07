package no.nav.tilbakekreving

import no.nav.familie.tilbake.common.exceptionhandler.IntegrasjonException
import no.nav.familie.tilbake.iverksettvedtak.domain.KodeResultat
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.behov.IverksettelseBehov
import no.nav.tilbakekreving.beregning.delperiode.Delperiode
import no.nav.tilbakekreving.beregning.delperiode.Foreldet
import no.nav.tilbakekreving.beregning.isZero
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
    private val oppdragRestClient: OppdragRestClient,
) {
    fun iverksett(
        iverksettelseBehov: IverksettelseBehov,
        logContext: SecureLog.Context,
    ): IverksattVedtak {
        val kravgrunnlagListe = kravgrunnlagBufferRepository.hentKravgrunnlag(iverksettelseBehov.kravgrunnlagId)
            .ifEmpty { error("Fant ikke kravgrunnlag for ${iverksettelseBehov.kravgrunnlagId}") }
            .map { KravgrunnlagUtil.unmarshalKravgrunnlag(it.kravgrunnlag) }

        val kravgrunnlag = kravgrunnlagListe.singleOrNull { it.kontrollfelt == iverksettelseBehov.kravgrunnlagInfo.kontrollfelt }
            ?: error("Kunne ikke finne kravgrunnlag med riktig kontrollfelt")

        val request = lagIverksettelseRequest(
            ansvarligSaksbehandler = iverksettelseBehov.ansvarligSaksbehandler,
            kravgrunnlag = kravgrunnlag,
            beregnetPerioder = iverksettelseBehov.delperioder,
        )
        log.medContext(logContext) {
            info("Iverksetter vedtak med REST-endepunkt")
        }
        val kvittering = oppdragRestClient.iverksettVedtak(request)
        validerIverksettelse(kvittering, logContext)
        return lagreIverksattVedtak(iverksettelseBehov, request, kvittering)
    }

    fun lagreIverksattVedtak(
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
            perioder = lagVedtaksperiode(beregnetPerioder, kravgrunnlag.tilbakekrevingsPeriode),
            datoTilleggsfrist = null,
        )
    }

    private fun lagVedtaksperiode(
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

    companion object {
        private val log = TracedLogger.getLogger<IverksettService>()

        fun validerIverksettelse(kvittering: TilbakekrevingsvedtakResponseDto, logContext: SecureLog.Context) {
            if (!OppdragRestClient.erResponseOk(kvittering.status)) {
                log.medContext(logContext) {
                    error("Fikk feil respons fra økonomi ved iverksetting. Mottatt respons: ${kvittering.status}, ${kvittering.melding}")
                }
                throw IntegrasjonException(
                    msg = "Fikk feil respons fra økonomi ved iverksetting. Mottatt respons: ${kvittering.status}, ${kvittering.melding}",
                    logContext = logContext,
                )
            }
        }
    }
}
