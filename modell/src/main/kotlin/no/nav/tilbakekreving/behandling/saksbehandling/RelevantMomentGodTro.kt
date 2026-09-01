package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.kontrakter.frontend.models.MomentDto
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.RelevantMomentTypeGodTro

sealed interface RelevantMomentGodTro : MomentEllerSærligGrunn<RelevantMomentTypeGodTro> {
    override val type: RelevantMomentTypeGodTro

    override fun tilFrontendDto() = MomentDto(
        moment = type.name,
        beskrivelse = type.navn,
    )

    object StørrelseBeløp : RelevantMomentGodTro {
        override val type: RelevantMomentTypeGodTro = RelevantMomentTypeGodTro.STØRRELSE_BELØP
    }

    object TidFraUtbetaling : RelevantMomentGodTro {
        override val type: RelevantMomentTypeGodTro = RelevantMomentTypeGodTro.TID_FRA_UTBETALING
    }

    object UtbetalingTillit : RelevantMomentGodTro {
        override val type: RelevantMomentTypeGodTro = RelevantMomentTypeGodTro.UTBETALING_TILLIT
    }

    data class Annet(val begrunnelse: String) : RelevantMomentGodTro {
        override val type: RelevantMomentTypeGodTro = RelevantMomentTypeGodTro.ANNET
    }
}
