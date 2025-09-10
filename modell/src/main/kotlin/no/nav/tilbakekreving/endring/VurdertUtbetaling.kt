package no.nav.tilbakekreving.endring

import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunn
import java.math.BigDecimal

data class VurdertUtbetaling(
    val periode: Datoperiode,
    val rettsligGrunnlag: String,
    val vilkårsvurdering: Vilkårsvurdering,
    val beregning: Beregning,
) {
    data class Vilkårsvurdering(
        // 1. ledd, 2. punktum
        val aktsomhetFørUtbetaling: Aktsomhet?,
        // 1. ledd, 1. punktum
        val aktsomhetEtterUtbetaling: Aktsomhet?,
        // Skille mellom 1. ledd 1. og 2. punktum
        val forårsaketAvBruker: ForårsaketAvBruker,
        // 4. ledd
        val særligeGrunner: SærligeGrunner?,
        // 6. ledd
        val beløpUnnlatesUnder4Rettsgebyr: JaNeiVurdering,
    )

    data class SærligeGrunner(
        val beløpReduseres: JaNeiVurdering,
        val grunner: Set<SærligGrunn>,
    )

    data class Beregning(
        val feilutbetaltBeløp: BigDecimal,
        val tilbakekrevesBeløp: BigDecimal,
        val rentebeløp: BigDecimal,
    )

    enum class ForårsaketAvBruker {
        IKKE_FORÅRSAKET_AV_BRUKER,
        MANGELFULLE_OPPLYSNINGER,
        FEILAKTIGE_OPPLYSNINGER,
        GOD_TRO,
    }

    enum class JaNeiVurdering {
        Ja,
        Nei,
    }
}
