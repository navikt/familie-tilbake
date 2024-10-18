package no.nav.familie.tilbake.beregning.modell

import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vurdering
import java.math.BigDecimal

data class Beregningsresultatsperiode(
    val periode: Månedsperiode,
    val vurdering: Vurdering? = null,
    val feilutbetaltBeløp: BigDecimal,
    val andelAvBeløp: BigDecimal? = null,
    val renteprosent: BigDecimal? = null,
    val manueltSattTilbakekrevingsbeløp: BigDecimal? = null,
    val tilbakekrevingsbeløpUtenRenter: BigDecimal,
    val rentebeløp: BigDecimal,
    val tilbakekrevingsbeløp: BigDecimal,
    val skattebeløp: BigDecimal,
    val tilbakekrevingsbeløpEtterSkatt: BigDecimal,
    // Rått beløp, ikke justert for ev. trekk
    val utbetaltYtelsesbeløp: BigDecimal,
    val riktigYtelsesbeløp: BigDecimal,
) // Rått beløp, ikke justert for ev. trekk

fun List<Beregningsresultatsperiode>.sammenslåOgSummer(): List<Beregningsresultatsperiode> =
    listOf(
        Beregningsresultatsperiode(
            periode = Månedsperiode(this.first().periode.fom, this.first().periode.tom),
            feilutbetaltBeløp = this.sumOf { it.feilutbetaltBeløp },
            vurdering = this.first().vurdering,
            andelAvBeløp = this.sumOf { it.andelAvBeløp ?: BigDecimal.ZERO },
            renteprosent = this.first().renteprosent,
            manueltSattTilbakekrevingsbeløp = this.first().manueltSattTilbakekrevingsbeløp,
            tilbakekrevingsbeløpUtenRenter = this.sumOf { it.tilbakekrevingsbeløpUtenRenter },
            rentebeløp = this.sumOf { it.rentebeløp },
            tilbakekrevingsbeløp = this.sumOf { it.tilbakekrevingsbeløp },
            skattebeløp = this.sumOf { it.skattebeløp },
            tilbakekrevingsbeløpEtterSkatt = this.sumOf { it.tilbakekrevingsbeløpEtterSkatt },
            utbetaltYtelsesbeløp = this.sumOf { it.utbetaltYtelsesbeløp },
            riktigYtelsesbeløp = this.sumOf { it.riktigYtelsesbeløp },
        ),
    )
