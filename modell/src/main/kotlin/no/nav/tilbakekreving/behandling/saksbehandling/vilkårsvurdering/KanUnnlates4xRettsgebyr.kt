package no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering

import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.endring.VurdertUtbetaling
import no.nav.tilbakekreving.entities.KanUnnlates

// §22-15 6. ledd
sealed interface KanUnnlates4xRettsgebyr {
    fun reduksjon(): Reduksjon

    fun oppsummering(): VurdertUtbetaling.JaNeiVurdering

    fun tilEntity(): KanUnnlates

    data object Unnlates : KanUnnlates4xRettsgebyr {
        override fun reduksjon(): Reduksjon = Reduksjon.IngenTilbakekreving()

        override fun oppsummering(): VurdertUtbetaling.JaNeiVurdering {
            return VurdertUtbetaling.JaNeiVurdering.Ja
        }

        override fun tilEntity(): KanUnnlates = KanUnnlates.UNNLATES
    }

    class SkalIkkeUnnlates(
        val reduksjonSærligeGrunner: ReduksjonSærligeGrunner,
    ) : KanUnnlates4xRettsgebyr {
        override fun reduksjon(): Reduksjon = reduksjonSærligeGrunner.skalReduseres.reduksjon()

        override fun oppsummering(): VurdertUtbetaling.JaNeiVurdering {
            return VurdertUtbetaling.JaNeiVurdering.Nei
        }

        override fun tilEntity(): KanUnnlates = KanUnnlates.SKAL_IKKE_UNNLATES
    }

    class ErOver4xRettsgebyr(
        val reduksjonSærligeGrunner: ReduksjonSærligeGrunner,
    ) : KanUnnlates4xRettsgebyr {
        override fun reduksjon(): Reduksjon {
            return reduksjonSærligeGrunner.skalReduseres.reduksjon()
        }

        override fun oppsummering(): VurdertUtbetaling.JaNeiVurdering {
            return VurdertUtbetaling.JaNeiVurdering.Nei
        }

        override fun tilEntity(): KanUnnlates = KanUnnlates.SKAL_IKKE_UNNLATES
    }
}
