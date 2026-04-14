package no.nav.tilbakekreving.test

import no.nav.tilbakekreving.test.vilkårsvurdering.AktsomhetBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.KanUnnlates4xRettsgebyrBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.ReduksjonSærligeGrunnerBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.Unnlates

fun uaktsomt(
    unnlates: KanUnnlates4xRettsgebyrBuilder = skalIkkeUnnlates(),
    reduksjon: ReduksjonSærligeGrunnerBuilder = ingenReduksjon(),
) = AktsomhetBuilder.Uaktsomt(unnlates, reduksjon).apply {
    this.unnlates = unnlates
}

fun grovtUaktsomt(
    unnlates: KanUnnlates4xRettsgebyrBuilder = skalIkkeUnnlates(),
    reduksjon: ReduksjonSærligeGrunnerBuilder = ingenReduksjon(),
) = AktsomhetBuilder.GrovtUaktsomt(unnlates, reduksjon)

fun forsettelig() = AktsomhetBuilder.Forsettelig

fun skalUnnlates(): KanUnnlates4xRettsgebyrBuilder = KanUnnlates4xRettsgebyrBuilder(unnlates = Unnlates.Unnlates)

fun skalIkkeUnnlates(): KanUnnlates4xRettsgebyrBuilder = KanUnnlates4xRettsgebyrBuilder(unnlates = Unnlates.Tilbakekreves)

fun over4Rettsgebyr(): KanUnnlates4xRettsgebyrBuilder = KanUnnlates4xRettsgebyrBuilder(unnlates = Unnlates.Over4Rettsgebyr)

fun ingenReduksjon() = ReduksjonSærligeGrunnerBuilder(skalReduseres = false, reduksjon = 0)

val Int.prosentReduksjon get() = ReduksjonSærligeGrunnerBuilder(skalReduseres = true, reduksjon = this)
