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
import no.nav.tilbakekreving.test.vilkårsvurdering.Unnlates
import no.nav.tilbakekreving.test.vilkårsvurdering.VilkårsvurderingProvider
import no.nav.tilbakekreving.test.vilkårsvurdering.VilkårsvurderingValgProvider

object VilkårsvurderingBuilderImpl : VilkårsvurderingProvider<ForårsaketAvBruker.Ja, ForårsaketAvBruker.Nei>, VilkårsvurderingValgProvider<KanUnnlates4xRettsgebyr, ReduksjonSærligeGrunner, KanUnnlates4xRettsgebyr> {
    override fun build(vurdering: ForårsaketAvNavBuilder.GodTroBuilder<ForårsaketAvBruker.Nei>): ForårsaketAvBruker.Nei {
        return NivåAvForståelse.GodTro(
            vurdering.beløpIBehold?.let(NivåAvForståelse.GodTro.BeløpIBehold::Ja) ?: NivåAvForståelse.GodTro.BeløpIBehold.Nei,
            "",
            "",
        )
    }

    override fun build(vurdering: ForårsaketAvNavBuilder.BurdeForstått<ForårsaketAvBruker.Nei>): ForårsaketAvBruker.Nei {
        return NivåAvForståelse.BurdeForstått(
            kanUnnlates4XRettsgebyr = vurdering.aktsomhet.build(this),
            begrunnelse = "",
        )
    }

    override fun build(vurdering: ForårsaketAvNavBuilder.Forstod<ForårsaketAvBruker.Nei>): ForårsaketAvBruker.Nei {
        return NivåAvForståelse.Forstod(begrunnelse = "")
    }

    override fun build(vurdering: ForårsaketAvBrukerBuilder.Uaktsomt<ForårsaketAvBruker.Ja>): ForårsaketAvBruker.Ja {
        return Skyldgrad.Uaktsomt(
            begrunnelse = "",
            begrunnelseAktsomhet = "",
            kanUnnlates4XRettsgebyr = vurdering.unnlates.build(this, vurdering.reduksjon),
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

    override fun build(unnlates: KanUnnlates4xRettsgebyrBuilder, reduksjon: ReduksjonSærligeGrunnerBuilder): KanUnnlates4xRettsgebyr {
        return when (unnlates.unnlates) {
            Unnlates.Unnlates -> KanUnnlates4xRettsgebyr.Unnlates
            Unnlates.Tilbakekreves -> KanUnnlates4xRettsgebyr.SkalIkkeUnnlates(reduksjon.build(this))
            Unnlates.Over4Rettsgebyr -> KanUnnlates4xRettsgebyr.ErOver4xRettsgebyr(reduksjon.build(this))
        }
    }

    override fun build(aktsomhet: AktsomhetBuilder.Uaktsomt): KanUnnlates4xRettsgebyr {
        return aktsomhet.unnlates.build(this, aktsomhet.reduksjon)
    }

    override fun build(aktsomhet: AktsomhetBuilder.GrovtUaktsomt): KanUnnlates4xRettsgebyr {
        return KanUnnlates4xRettsgebyr.SkalIkkeUnnlates(aktsomhet.reduksjon.build(this))
    }

    override fun build(aktsomhet: AktsomhetBuilder.Forsettelig): KanUnnlates4xRettsgebyr {
        return KanUnnlates4xRettsgebyr.SkalIkkeUnnlates(
            ReduksjonSærligeGrunner(
                begrunnelse = "",
                grunner = emptySet(),
                skalReduseres = ReduksjonSærligeGrunner.SkalReduseres.Nei,
            ),
        )
    }
}
