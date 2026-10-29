package no.nav.tilbakekreving.builders

import no.nav.tilbakekreving.api.v1.dto.AktsomhetDto
import no.nav.tilbakekreving.api.v1.dto.GodTroDto
import no.nav.tilbakekreving.api.v1.dto.SkalUnnlates
import no.nav.tilbakekreving.api.v1.dto.VilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.test.TestdataProvider
import no.nav.tilbakekreving.test.ingenReduksjon
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.skalIkkeUnnlates
import no.nav.tilbakekreving.test.vilkårsvurdering.AktsomhetBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.BeløpIBeholdBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.ForårsaketAvBrukerBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.ForårsaketAvNavBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.KanUnnlates4xRettsgebyrBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.ReduksjonSærligeGrunnerBuilder
import no.nav.tilbakekreving.test.vilkårsvurdering.Unnlates
import no.nav.tilbakekreving.test.vilkårsvurdering.VilkårsvurderingProvider
import no.nav.tilbakekreving.test.vilkårsvurdering.VilkårsvurderingValgProvider

object VilkårsvurderingDtoBuilder :
    TestdataProvider<VilkårsvurderingsperiodeDto, VilkårsvurderingsperiodeDto, VilkårsvurderingDtoBuilder>,
    VilkårsvurderingProvider<VilkårsvurderingsperiodeDto, VilkårsvurderingsperiodeDto>,
    VilkårsvurderingValgProvider<AktsomhetDto, AktsomhetDto, AktsomhetDto> {
    override val provider: VilkårsvurderingDtoBuilder = this

    override fun build(vurdering: ForårsaketAvNavBuilder.GodTroBuilder<VilkårsvurderingsperiodeDto>): VilkårsvurderingsperiodeDto {
        return VilkårsvurderingsperiodeDto(
            periode = 1.januar(2021) til 31.januar(2021),
            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
            begrunnelse = "",
            godTroDto = when (val beløpIBehold = vurdering.beløpIBehold) {
                BeløpIBeholdBuilder.Ingenting -> GodTroDto(beløpErIBehold = false, beløpTilbakekreves = null, begrunnelse = "")
                is BeløpIBeholdBuilder.Deler -> GodTroDto(beløpErIBehold = true, beløpTilbakekreves = beløpIBehold.beløp, begrunnelse = "")
                BeløpIBeholdBuilder.Hele -> error("Gammel modell støtter ikke at hele beløpet er i behold")
            },
            aktsomhetDto = null,
        )
    }

    override fun build(vurdering: ForårsaketAvNavBuilder.BurdeForstått<VilkårsvurderingsperiodeDto>): VilkårsvurderingsperiodeDto {
        return VilkårsvurderingsperiodeDto(
            periode = 1.januar(2021) til 31.januar(2021),
            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
            begrunnelse = "",
            godTroDto = null,
            aktsomhetDto = vurdering.aktsomhet.build(this),
        )
    }

    override fun build(vurdering: ForårsaketAvNavBuilder.Forstod<VilkårsvurderingsperiodeDto>): VilkårsvurderingsperiodeDto {
        return VilkårsvurderingsperiodeDto(
            periode = 1.januar(2021) til 31.januar(2021),
            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
            begrunnelse = "",
            godTroDto = null,
            aktsomhetDto = vurdering.aktsomhet.build(this),
        )
    }

    override fun build(vurdering: ForårsaketAvBrukerBuilder.Uaktsomt<VilkårsvurderingsperiodeDto>): VilkårsvurderingsperiodeDto {
        return VilkårsvurderingsperiodeDto(
            periode = 1.januar(2021) til 31.januar(2021),
            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
            begrunnelse = "",
            godTroDto = null,
            aktsomhetDto = build(vurdering.unnlates, vurdering.reduksjon)
                .copy(aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET),
        )
    }

    override fun build(vurdering: ForårsaketAvBrukerBuilder.GrovtUaktsomt<VilkårsvurderingsperiodeDto>): VilkårsvurderingsperiodeDto {
        return VilkårsvurderingsperiodeDto(
            periode = 1.januar(2021) til 31.januar(2021),
            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
            begrunnelse = "",
            godTroDto = null,
            aktsomhetDto = build(vurdering.unnlates, vurdering.reduksjon)
                .copy(
                    aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                    ileggRenter = true,
                ),
        )
    }

    override fun build(vurdering: ForårsaketAvBrukerBuilder.Forsettelig<VilkårsvurderingsperiodeDto>): VilkårsvurderingsperiodeDto {
        return VilkårsvurderingsperiodeDto(
            periode = 1.januar(2021) til 31.januar(2021),
            vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
            begrunnelse = "",
            godTroDto = null,
            aktsomhetDto = build(skalIkkeUnnlates(), ingenReduksjon())
                .copy(
                    aktsomhet = Aktsomhet.FORSETT,
                    ileggRenter = true,
                ),
        )
    }

    override fun build(unnlates: KanUnnlates4xRettsgebyrBuilder, reduksjon: ReduksjonSærligeGrunnerBuilder): AktsomhetDto {
        return AktsomhetDto(
            aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
            ileggRenter = false,
            andelTilbakekreves = reduksjon.reduksjon.toBigDecimal(),
            beløpTilbakekreves = null,
            begrunnelse = "",
            særligeGrunner = emptyList(),
            særligeGrunnerTilReduksjon = reduksjon.skalReduseres,
            unnlates4Rettsgebyr = when (unnlates.unnlates) {
                Unnlates.Unnlates -> SkalUnnlates.UNNLATES
                Unnlates.Tilbakekreves -> SkalUnnlates.TILBAKEKREVES
                Unnlates.Over4Rettsgebyr -> SkalUnnlates.OVER_4_RETTSGEBYR
            },
            særligeGrunnerBegrunnelse = "",
        )
    }

    override fun build(reduksjon: ReduksjonSærligeGrunnerBuilder): AktsomhetDto {
        return build(skalIkkeUnnlates(), reduksjon)
    }

    override fun build(aktsomhet: AktsomhetBuilder.Uaktsomt): AktsomhetDto {
        return build(aktsomhet.unnlates, aktsomhet.reduksjon)
            .copy(aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET)
    }

    override fun build(aktsomhet: AktsomhetBuilder.GrovtUaktsomt): AktsomhetDto {
        return build(aktsomhet.unnlates, aktsomhet.reduksjon)
            .copy(aktsomhet = Aktsomhet.GROV_UAKTSOMHET)
    }

    override fun build(aktsomhet: AktsomhetBuilder.Forsettelig): AktsomhetDto {
        return build(skalIkkeUnnlates(), ingenReduksjon())
            .copy(
                aktsomhet = Aktsomhet.FORSETT,
                ileggRenter = true,
            )
    }
}
