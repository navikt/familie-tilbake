package no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering

import no.nav.tilbakekreving.beregning.Reduksjon

// §22-15 6. ledd
sealed interface KanUnnlates4xRettsgebyr {
    fun reduksjon(): Reduksjon

    data object Unnlates : KanUnnlates4xRettsgebyr {
        override fun reduksjon(): Reduksjon = Reduksjon.IngenTilbakekreving()
    }

    class SkalIkkeUnnlates : KanUnnlates4xRettsgebyr {
        override fun reduksjon(): Reduksjon = Reduksjon.FullstendigTilbakekreving()
    }

    class ErOver4xRettsgebyr(
        val reduksjonSærligeGrunner: ReduksjonSærligeGrunner,
    ) : KanUnnlates4xRettsgebyr {
        override fun reduksjon(): Reduksjon {
            return reduksjonSærligeGrunner.skalReduseres.reduksjon()
        }
    }
}
