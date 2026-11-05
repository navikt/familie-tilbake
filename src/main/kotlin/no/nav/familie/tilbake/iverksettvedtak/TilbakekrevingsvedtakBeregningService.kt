package no.nav.familie.tilbake.iverksettvedtak

import no.nav.familie.tilbake.beregning.TilbakekrevingsberegningService
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import no.nav.tilbakekreving.beregning.delperiode.Delperiode
import no.nav.tilbakekreving.beregning.delperiode.Foreldet
import no.nav.tilbakekreving.beregning.isZero
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.PosteringDto
import no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter.VedtakPeriodeDto
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Service
class TilbakekrevingsvedtakBeregningService(
    private val tilbakekrevingsberegningService: TilbakekrevingsberegningService,
) {
    fun beregnVedtaksperioder(
        behandlingId: UUID,
        kravgrunnlag431: Kravgrunnlag431,
    ): List<VedtakPeriodeDto> {
        val delperioder = tilbakekrevingsberegningService.beregn(behandlingId).beregn()

        val kravgrunnlagsperioder = kravgrunnlag431.perioder.toList().sortedBy { it.periode.fom }
        val beregnetePerioder = delperioder.sortedBy { it.periode.fom }

        return beregnetePerioder
            .map { beregnetPeriode -> lagTilbakekrevingsperioder(kravgrunnlagsperioder, beregnetPeriode) }
            .flatten()
    }

    private fun lagTilbakekrevingsperioder(
        kravgrunnlagsperioder: List<Kravgrunnlagsperiode432>,
        beregnetPeriode: Delperiode<out Delperiode.Beløp>,
    ): List<VedtakPeriodeDto> =
        kravgrunnlagsperioder
            .filter { it.periode.snitt(beregnetPeriode.periode.toMånedsperiode()) != null }
            .map {
                VedtakPeriodeDto(
                    periodeFom = it.periode.fom.atDay(1),
                    periodeTom = it.periode.tom.atEndOfMonth(),
                    renterPeriodeBeregnes = false,
                    belopRenter = beregnetPeriode.renter(),
                    posteringer = lagTilbakekrevingsbeløp(it.beløp, beregnetPeriode),
                )
            }

    private fun lagTilbakekrevingsbeløp(
        kravgrunnlagsbeløp: Set<Kravgrunnlagsbeløp433>,
        beregnetPeriode: Delperiode<out Delperiode.Beløp>,
    ): List<PosteringDto> =
        kravgrunnlagsbeløp.mapNotNull {
            when (it.klassetype) {
                Klassetype.FEIL ->
                    PosteringDto(
                        kodeKlasse = it.klassekode.tilKlassekodeNavn(),
                        belopNy = it.nyttBeløp.setScale(0, RoundingMode.HALF_UP),
                        belopOpprinneligUtbetalt = BigDecimal.ZERO,
                        belopTilbakekreves = BigDecimal.ZERO,
                        belopUinnkrevd = BigDecimal.ZERO,
                        belopSkatt = BigDecimal.ZERO,
                        kodeResultat = "",
                        kodeAarsak = "", // fast verdi
                        kodeSkyld = "", // fast verdi
                    )

                Klassetype.YTEL -> {
                    val beløp = beregnetPeriode.beløpForKlassekode(it.klassekode.tilKlassekodeNavn())
                    PosteringDto(
                        kodeKlasse = it.klassekode.tilKlassekodeNavn(),
                        belopNy = it.nyttBeløp.setScale(0, RoundingMode.HALF_UP),
                        belopOpprinneligUtbetalt = beløp.utbetaltYtelsesbeløp(),
                        belopTilbakekreves = beløp.tilbakekrevesBrutto(),
                        belopUinnkrevd = it.tilbakekrevesBeløp
                            .subtract(beløp.tilbakekrevesBrutto())
                            .setScale(0, RoundingMode.HALF_UP),
                        belopSkatt = beløp.skatt(),
                        kodeResultat = utledKodeResulat(beregnetPeriode),
                        kodeAarsak = "ANNET", // fast verdi
                        kodeSkyld = "IKKE_FORDELT", // fast verdi
                    )
                }

                else -> null
            }
        }

    companion object {
        fun utledKodeResulat(beregnetPeriode: Delperiode<out Delperiode.Beløp>): String = when {
            beregnetPeriode is Foreldet.ForeldetPeriode -> "FORELDET"
            beregnetPeriode.beløp().sumOf { it.tilbakekrevesBrutto() }.isZero() -> "INGEN_TILBAKEKREV"
            beregnetPeriode.feilutbetaltBeløp() == beregnetPeriode.beløp().sumOf { it.tilbakekrevesBrutto() } -> "FULL_TILBAKEKREV"
            else -> "DELVIS_TILBAKEKREV"
        }
    }
}
