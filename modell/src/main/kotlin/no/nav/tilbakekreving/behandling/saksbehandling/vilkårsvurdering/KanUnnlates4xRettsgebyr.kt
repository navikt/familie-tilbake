package no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering

import no.nav.tilbakekreving.beregning.Reduksjon

sealed interface KanUnnlates4xRettsgebyr {
    fun reduksjon(): Reduksjon

    data object Unnlates : KanUnnlates4xRettsgebyr {
        override fun reduksjon(): Reduksjon = Reduksjon.IngenTilbakekreving()
    }

    class Tilbakekreves(
        val reduksjonSærligeGrunner: ReduksjonSærligeGrunner,
    ) : KanUnnlates4xRettsgebyr {
        override fun reduksjon(): Reduksjon {
            return reduksjonSærligeGrunner.skalReduseres.reduksjon()
        }
    }
}
