package no.nav.tilbakekreving.test

import no.nav.tilbakekreving.test.vilkårsvurdering.AktsomhetBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.KanUnnlates4xRettsgebyrBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.ReduksjonSærligeGrunnerBuilder

fun uaktsomt(unnlates: KanUnnlates4xRettsgebyrBuilder = skalIkkeUnnlates()) = AktsomhetBuilder.Uaktsomt().apply {
    this.unnlates = unnlates
}

fun forsettelig() = AktsomhetBuilder.Forsettelig

fun skalUnnlates(): KanUnnlates4xRettsgebyrBuilder = KanUnnlates4xRettsgebyrBuilder().apply {
    this.unnlates = true
}

fun skalIkkeUnnlates(reduksjon: ReduksjonSærligeGrunnerBuilder = ingenReduksjon()): KanUnnlates4xRettsgebyrBuilder = KanUnnlates4xRettsgebyrBuilder().apply {
    this.unnlates = false
    this.reduksjon = reduksjon
}

fun ingenReduksjon() = ReduksjonSærligeGrunnerBuilder().apply {
    this.skalReduseres = false
}

val Int.prosentReduksjon get() = ReduksjonSærligeGrunnerBuilder().apply {
    skalReduseres = true
    reduksjon = this@prosentReduksjon
}
