package no.nav.tilbakekreving.test.vilkårsvurdering

interface VilkårsvurderingValgProvider<BuiltUnnlates4xRettsgebyr, BuiltReduksjonSærligeGrunner, BuiltAktsomhet> {
    fun build(unnlates: KanUnnlates4xRettsgebyrBuilder): BuiltUnnlates4xRettsgebyr

    fun build(reduksjon: ReduksjonSærligeGrunnerBuilder): BuiltReduksjonSærligeGrunner

    fun build(aktsomhet: AktsomhetBuilder.Uaktsomt): BuiltAktsomhet

    fun build(aktsomhet: AktsomhetBuilder.GrovtUaktsomt): BuiltAktsomhet

    fun build(aktsomhet: AktsomhetBuilder.Forsettelig): BuiltAktsomhet
}
