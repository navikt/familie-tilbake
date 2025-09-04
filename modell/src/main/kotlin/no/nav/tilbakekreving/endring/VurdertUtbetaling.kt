package no.nav.tilbakekreving.endring

import java.math.BigDecimal
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunn

data class VurdertUtbetaling(
    val periode: Datoperiode,
    val rettsligGrunnlag: String,
    val vilkårsvurdering: Vilkårsvurdering,
    val beregning: Beregning,
) {
    data class Vilkårsvurdering(
        // 1. ledd, 2. punktum
        val aktsomhetFørUtbetaling: Aktsomhet,
        // 1. ledd, 1. punktum
        val aktsomhetEtterUtbetaling: Aktsomhet?,
        // 4. ledd
        val særligeGrunner: SærligeGrunner?,
        // 6. ledd
        val beløpUnnlatesUnder4Rettsgebyr: Boolean
    )

    data class SærligeGrunner(
        val beløpReduseres: Reduseres,
        val grunner: Set<SærligGrunn>
    ) {
        enum class Reduseres {
            Ja,
            Nei
        }
    }

    data class Beregning(
        val feilutbetaltBeløp: BigDecimal,
        val tilbakekrevesBeløp: BigDecimal,
        val rentebeløp: BigDecimal,
    )
}
