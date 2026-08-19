package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.RelevanteMomentGodTro
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.RelevanteMomentType

data class RelevanteMomentGodTroEntity(
    val type: RelevanteMomentType,
    val annetBegrunnelse: String?,
) {
    fun fraEntity(): RelevanteMomentGodTro {
        return when (type) {
            RelevanteMomentType.STØRRELSE_BELØP -> RelevanteMomentGodTro.StørrelseBeløp
            RelevanteMomentType.TID_FRA_UTBETALING -> RelevanteMomentGodTro.TidFraUtbetaling
            RelevanteMomentType.UTBETALING_TILLIT -> RelevanteMomentGodTro.UtbetalingTillit
            RelevanteMomentType.ANNET -> RelevanteMomentGodTro.Annet(requireNotNull(annetBegrunnelse))
        }
    }
}
