package no.nav.tilbakekreving.builders

import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ForårsaketAvBruker
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.KanUnnlates4xRettsgebyr
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.NivåAvForståelse
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.ReduksjonSærligeGrunner
import no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering.Skyldgrad
import no.nav.tilbakekreving.test.vilkårsvurdering.AktsomhetBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.ForårsaketAvBrukerBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.ForårsaketAvNavBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.KanUnnlates4xRettsgebyrBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.ReduksjonSærligeGrunnerBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.VilkårsvurderingProvider
import no.nav.tilbakekreving.test.vilkårsvurdering.VilkårsvurderingValgProvider

object VilkårsvurderingBuilderImpl : VilkårsvurderingProvider<ForårsaketAvBruker.Ja, ForårsaketAvBruker.Nei>, VilkårsvurderingValgProvider<KanUnnlates4xRettsgebyr, ReduksjonSærligeGrunner, NivåAvForståelse.Aktsomhet> {
    override fun build(vurdering: ForårsaketAvNavBuilder.GodTroBuilder<ForårsaketAvBruker.Nei>): ForårsaketAvBruker.Nei {
        return NivåAvForståelse.GodTro(
            vurdering.beløpIBehold?.let(NivåAvForståelse.GodTro.BeløpIBehold::Ja) ?: NivåAvForståelse.GodTro.BeløpIBehold.Nei,
            "",
            "",
        )
    }

    override fun build(vurdering: ForårsaketAvNavBuilder.BurdeForstått<ForårsaketAvBruker.Nei>): ForårsaketAvBruker.Nei {
        return NivåAvForståelse.BurdeForstått(
            aktsomhet = vurdering.aktsomhet.build(this),
            begrunnelse = "",
        )
    }

    override fun build(vurdering: ForårsaketAvNavBuilder.Forstod<ForårsaketAvBruker.Nei>): ForårsaketAvBruker.Nei {
        return NivåAvForståelse.Forstod(
            aktsomhet = vurdering.aktsomhet.build(this),
            begrunnelse = "",
        )
    }

    override fun build(vurdering: ForårsaketAvBrukerBuilder.Uaktsomt<ForårsaketAvBruker.Ja>): ForårsaketAvBruker.Ja {
        return Skyldgrad.Uaktsomt(
            begrunnelse = "",
            begrunnelseAktsomhet = "",
            kanUnnlates4XRettsgebyr = vurdering.skalUnnlates.build(this),
            feilaktigeEllerMangelfulleOpplysninger = Skyldgrad.FeilaktigEllerMangelfull.FEILAKTIG,
        )
    }

    override fun build(vurdering: ForårsaketAvBrukerBuilder.GrovtUaktsomt<ForårsaketAvBruker.Ja>): ForårsaketAvBruker.Ja {
        return Skyldgrad.GrovUaktsomhet(
            "",
            "",
            reduksjonSærligeGrunner = vurdering.reduksjon.build(this),
            Skyldgrad.FeilaktigEllerMangelfull.FEILAKTIG,
        )
    }

    override fun build(vurdering: ForårsaketAvBrukerBuilder.Forsettelig<ForårsaketAvBruker.Ja>): ForårsaketAvBruker.Ja {
        return Skyldgrad.Forsett(
            begrunnelse = "",
            begrunnelseAktsomhet = "",
            feilaktigeEllerMangelfulleOpplysninger = Skyldgrad.FeilaktigEllerMangelfull.FEILAKTIG,
        )
    }

    override fun build(reduksjon: ReduksjonSærligeGrunnerBuilder): ReduksjonSærligeGrunner {
        return ReduksjonSærligeGrunner(
            begrunnelse = "",
            grunner = emptySet(),
            skalReduseres = when (reduksjon.skalReduseres) {
                true -> ReduksjonSærligeGrunner.SkalReduseres.Ja(reduksjon.reduksjon)
                false -> ReduksjonSærligeGrunner.SkalReduseres.Nei
            },
        )
    }

    override fun build(unnlates: KanUnnlates4xRettsgebyrBuilder): KanUnnlates4xRettsgebyr {
        return when (unnlates.unnlates) {
            true -> KanUnnlates4xRettsgebyr.Unnlates
            false -> KanUnnlates4xRettsgebyr.SkalIkkeUnnlates(
                unnlates.reduksjon.build(this),
            )
        }
    }

    override fun build(aktsomhet: AktsomhetBuilder.Uaktsomt): NivåAvForståelse.Aktsomhet {
        return NivåAvForståelse.Aktsomhet.Uaktsomhet(
            kanUnnlates4XRettsgebyr = aktsomhet.unnlates.build(this),
            begrunnelse = "",
        )
    }

    override fun build(aktsomhet: AktsomhetBuilder.GrovtUaktsomt): NivåAvForståelse.Aktsomhet {
        return NivåAvForståelse.Aktsomhet.GrovUaktsomhet(
            reduksjonSærligeGrunner = aktsomhet.reduksjon.build(this),
            begrunnelse = "",
        )
    }

    override fun build(aktsomhet: AktsomhetBuilder.Forsettelig): NivåAvForståelse.Aktsomhet {
        return NivåAvForståelse.Aktsomhet.Forsett("")
    }
}
