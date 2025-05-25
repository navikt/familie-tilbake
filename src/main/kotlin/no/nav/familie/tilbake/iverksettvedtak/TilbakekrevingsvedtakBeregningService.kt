package no.nav.familie.tilbake.iverksettvedtak

import no.nav.familie.tilbake.beregning.TilbakekrevingsberegningService
import no.nav.familie.tilbake.iverksettvedtak.domain.KodeResultat
import no.nav.familie.tilbake.iverksettvedtak.domain.Tilbakekrevingsbeløp
import no.nav.familie.tilbake.iverksettvedtak.domain.Tilbakekrevingsperiode
import no.nav.familie.tilbake.kravgrunnlag.domain.Klassetype
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsbeløp433
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlagsperiode432
import no.nav.tilbakekreving.beregning.delperiode.Delperiode
import no.nav.tilbakekreving.beregning.delperiode.Foreldet
import no.nav.tilbakekreving.beregning.isZero
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
        val delperioder = tilbakekrevingsberegningService.beregn(behandlingId).beregn()

        val kravgrunnlagsperioder = kravgrunnlag431.perioder.toList().sortedBy { it.periode.fom }
        val beregnetePerioder = delperioder.sortedBy { it.periode.fom }

        return beregnetePerioder
            .map { beregnetPeriode -> lagTilbakekrevingsperioder(kravgrunnlagsperioder, beregnetPeriode) }
            .flatten()
    }

    private fun lagTilbakekrevingsperioder(
        kravgrunnlagsperioder: List<Kravgrunnlagsperiode432>,
        beregnetPeriode: Delperiode,
    ): List<Tilbakekrevingsperiode> =
        kravgrunnlagsperioder
            .filter { it.periode.snitt(beregnetPeriode.periode.toMånedsperiode()) != null }
            .map { Tilbakekrevingsperiode(it.periode, beregnetPeriode.renter(), lagTilbakekrevingsbeløp(it.beløp, beregnetPeriode)) }

    private fun lagTilbakekrevingsbeløp(
        kravgrunnlagsbeløp: Set<Kravgrunnlagsbeløp433>,
        beregnetPeriode: Delperiode,
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
                    val beløp = beregnetPeriode.beløpForKlassekode(it.klassekode.tilKlassekodeNavn())
                    Tilbakekrevingsbeløp(
                        klassetype = it.klassetype,
                        klassekode = it.klassekode,
                        nyttBeløp = it.nyttBeløp.setScale(0, RoundingMode.HALF_UP),
                        utbetaltBeløp = beløp.utbetaltYtelsesbeløp(),
                        tilbakekrevesBeløp = beløp.tilbakekrevesBrutto(),
                        uinnkrevdBeløp = it.tilbakekrevesBeløp
                            .subtract(beløp.tilbakekrevesBrutto())
                            .setScale(0, RoundingMode.HALF_UP),
                        skattBeløp = beløp.skatt(),
                        kodeResultat = utledKodeResulat(beregnetPeriode),
                    )
                }

                else -> null
            }
        }

    private fun utledKodeResulat(beregnetPeriode: Delperiode): KodeResultat = when {
        beregnetPeriode is Foreldet.ForeldetPeriode -> KodeResultat.FORELDET
        beregnetPeriode.beløp().sumOf { it.tilbakekrevesBrutto() }.isZero() -> KodeResultat.INGEN_TILBAKEKREVING
        beregnetPeriode.feilutbetaltBeløp() == beregnetPeriode.beløp().sumOf { it.tilbakekrevesBrutto() } -> KodeResultat.FULL_TILBAKEKREVING
        else -> KodeResultat.DELVIS_TILBAKEKREVING
    }
}
