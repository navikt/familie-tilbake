package no.nav.familie.tilbake.iverksettvedtak

import no.nav.familie.tilbake.beregning.TilbakekrevingsberegningService
import no.nav.familie.tilbake.iverksettvedtak.domain.KodeResultat
import no.nav.familie.tilbake.iverksettvedtak.domain.Tilbakekrevingsbeløp
import no.nav.familie.tilbake.iverksettvedtak.domain.Tilbakekrevingsperiode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import no.nav.tilbakekreving.beregning.isZero
import no.nav.tilbakekreving.beregning.modell.Beregningsresultatsperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.AnnenVurdering
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
    ): List<Tilbakekrevingsperiode> {
        val beregningsresultat = tilbakekrevingsberegningService.beregn(behandlingId)

        val kravgrunnlagsperioder = kravgrunnlag431.perioder.toList().sortedBy { it.periode.fom }
        val beregnetePerioder = beregningsresultat.beregningsresultatsperioder.sortedBy { it.periode.fom }

        return beregnetePerioder
            .map { beregnetPeriode -> lagTilbakekrevingsperioder(kravgrunnlagsperioder, beregnetPeriode) }
            .flatten()
    }

    private fun lagTilbakekrevingsperioder(
        kravgrunnlagsperioder: List<Kravgrunnlagsperiode432>,
        beregnetPeriode: Beregningsresultatsperiode,
    ): List<Tilbakekrevingsperiode> =
        kravgrunnlagsperioder
            .filter { it.periode.snitt(beregnetPeriode.periode.toMånedsperiode()) != null }
            .map { Tilbakekrevingsperiode(it.periode, beregnetPeriode.rentebeløp, lagTilbakekrevingsbeløp(it.beløp, beregnetPeriode)) }

    private fun lagTilbakekrevingsbeløp(
        kravgrunnlagsbeløp: Set<Kravgrunnlagsbeløp433>,
        beregnetPeriode: Beregningsresultatsperiode,
    ): List<Tilbakekrevingsbeløp> =
        kravgrunnlagsbeløp.mapNotNull {
            when (it.klassetype) {
                Klassetype.FEIL ->
                    Tilbakekrevingsbeløp(
                        klassetype = it.klassetype,
                        klassekode = it.klassekode,
                        nyttBeløp = it.nyttBeløp.setScale(0, RoundingMode.HALF_UP),
                        utbetaltBeløp = BigDecimal.ZERO,
                        tilbakekrevesBeløp = BigDecimal.ZERO,
                        uinnkrevdBeløp = BigDecimal.ZERO,
                        skattBeløp = BigDecimal.ZERO,
                        kodeResultat = utledKodeResulat(beregnetPeriode),
                    )

                Klassetype.YTEL -> {
                    Tilbakekrevingsbeløp(
                        klassetype = it.klassetype,
                        klassekode = it.klassekode,
                        nyttBeløp = it.nyttBeløp.setScale(0, RoundingMode.HALF_UP),
                        utbetaltBeløp = beregnetPeriode.utbetaltYtelsesbeløp,
                        tilbakekrevesBeløp = beregnetPeriode.tilbakekrevingsbeløpUtenRenter,
                        uinnkrevdBeløp = it.tilbakekrevesBeløp
                            .subtract(beregnetPeriode.tilbakekrevingsbeløpUtenRenter)
                            .setScale(0, RoundingMode.HALF_UP),
                        skattBeløp = beregnetPeriode.skattebeløp,
                        kodeResultat = utledKodeResulat(beregnetPeriode),
                    )
                }

                else -> null
            }
        }

    private fun utledKodeResulat(beregnetPeriode: Beregningsresultatsperiode): KodeResultat =
        when {
            beregnetPeriode.vurdering == AnnenVurdering.FORELDET -> {
                KodeResultat.FORELDET
            }

            beregnetPeriode.tilbakekrevingsbeløpUtenRenter.isZero() -> {
                KodeResultat.INGEN_TILBAKEKREVING
            }

            beregnetPeriode.feilutbetaltBeløp == beregnetPeriode.tilbakekrevingsbeløpUtenRenter -> {
                KodeResultat.FULL_TILBAKEKREVING
            }

            else -> KodeResultat.DELVIS_TILBAKEKREVING
        }
}
