package no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering

import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.endring.VurdertUtbetaling

// §22-15 6. ledd
sealed interface KanUnnlates4xRettsgebyr {
    val kanUnnlates: KanUnnlates

    fun reduksjon(): Reduksjon

    fun oppsummering(): VurdertUtbetaling.JaNeiVurdering

    data object Unnlates : KanUnnlates4xRettsgebyr {
        override val kanUnnlates = KanUnnlates.Ja

        override fun reduksjon(): Reduksjon = Reduksjon.IngenTilbakekreving()

        override fun oppsummering(): VurdertUtbetaling.JaNeiVurdering {
            return VurdertUtbetaling.JaNeiVurdering.Ja
        }
    }

    class SkalIkkeUnnlates : KanUnnlates4xRettsgebyr {
        override val kanUnnlates = KanUnnlates.Nei

        override fun reduksjon(): Reduksjon = Reduksjon.FullstendigTilbakekreving()

        override fun oppsummering(): VurdertUtbetaling.JaNeiVurdering {
            return VurdertUtbetaling.JaNeiVurdering.Nei
        }
    }

    class ErOver4xRettsgebyr(
        val reduksjonSærligeGrunner: ReduksjonSærligeGrunner,
    ) : KanUnnlates4xRettsgebyr {
        override val kanUnnlates = KanUnnlates.Nei

        override fun reduksjon(): Reduksjon {
            return reduksjonSærligeGrunner.skalReduseres.reduksjon()
        }

        override fun oppsummering(): VurdertUtbetaling.JaNeiVurdering {
            return VurdertUtbetaling.JaNeiVurdering.Nei
        }
    }

    enum class KanUnnlates {
        Ja,
        Nei,
    }
}
