package no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering

import no.nav.tilbakekreving.api.v1.dto.VurdertAktsomhetDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsresultatDto
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.entities.AktsomhetsvurderingEntity
import no.nav.tilbakekreving.entities.VurderingType
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vurdering

sealed interface Skyldgrad : ForårsaketAvBruker.Ja {
    class SimpelUaktsomhet(
        override val begrunnelse: String,
        private val reduksjonSærligeGrunner: ReduksjonSærligeGrunner,
    ) : Skyldgrad {
        override fun renter() = false

        override fun reduksjon(): Reduksjon = reduksjonSærligeGrunner.skalReduseres.reduksjon()

        override fun vurderingstype(): Vurdering = Aktsomhet.SIMPEL_UAKTSOMHET

        override fun tilFrontendDto(): VurdertVilkårsvurderingsresultatDto? {
            return VurdertVilkårsvurderingsresultatDto(
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER,
                aktsomhet = VurdertAktsomhetDto(
                    aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                    ileggRenter = renter(),
                    andelTilbakekreves = reduksjon().andel,
                    beløpTilbakekreves = null,
                    begrunnelse = begrunnelse,
                    særligeGrunner = reduksjonSærligeGrunner.vurderteGrunner(),
                    særligeGrunnerTilReduksjon = reduksjonSærligeGrunner.skalReduseres is ReduksjonSærligeGrunner.SkalReduseres.Ja,
                    tilbakekrevSmåbeløp = true,
                    særligeGrunnerBegrunnelse = reduksjonSærligeGrunner.begrunnelse,
                ),
            )
        }

        override fun tilEntity(): AktsomhetsvurderingEntity {
            return AktsomhetsvurderingEntity(
                vurderingType = VurderingType.FORÅRSAKET_AV_BRUKER_SIMPEL_UAKTSOMHET,
                begrunnelse = begrunnelse,
                beløpIBehold = null,
                aktsomhet = null,
            )
        }
    }

    class GrovUaktsomhet(
        override val begrunnelse: String,
        private val reduksjonSærligeGrunner: ReduksjonSærligeGrunner,
    ) : Skyldgrad {
        override fun vurderingstype(): Vurdering = Aktsomhet.GROV_UAKTSOMHET

        override fun renter() = true

        override fun reduksjon(): Reduksjon = reduksjonSærligeGrunner.skalReduseres.reduksjon()

        override fun tilFrontendDto(): VurdertVilkårsvurderingsresultatDto? {
            return VurdertVilkårsvurderingsresultatDto(
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER,
                aktsomhet = VurdertAktsomhetDto(
                    aktsomhet = Aktsomhet.GROV_UAKTSOMHET,
                    ileggRenter = renter(),
                    andelTilbakekreves = reduksjon().andel,
                    beløpTilbakekreves = null,
                    begrunnelse = begrunnelse,
                    særligeGrunner = reduksjonSærligeGrunner.vurderteGrunner(),
                    særligeGrunnerTilReduksjon = reduksjonSærligeGrunner.skalReduseres is ReduksjonSærligeGrunner.SkalReduseres.Ja,
                    tilbakekrevSmåbeløp = true,
                    særligeGrunnerBegrunnelse = reduksjonSærligeGrunner.begrunnelse,
                ),
            )
        }

        override fun tilEntity(): AktsomhetsvurderingEntity {
            return AktsomhetsvurderingEntity(
                vurderingType = VurderingType.FORÅRSAKET_AV_BRUKER_GROV_UAKTSOMHET,
                begrunnelse = begrunnelse,
                beløpIBehold = null,
                aktsomhet = null,
            )
        }
    }

    class Forsett(
        override val begrunnelse: String,
    ) : Skyldgrad {
        override fun renter() = true

        override fun vurderingstype(): Vurdering = Aktsomhet.FORSETT

        override fun reduksjon(): Reduksjon = Reduksjon.FullstendigRefusjon()

        override fun tilFrontendDto(): VurdertVilkårsvurderingsresultatDto? {
            return VurdertVilkårsvurderingsresultatDto(
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER,
                aktsomhet = VurdertAktsomhetDto(
                    aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                    ileggRenter = renter(),
                    andelTilbakekreves = reduksjon().andel,
                    beløpTilbakekreves = null,
                    begrunnelse = begrunnelse,
                    særligeGrunner = null,
                    særligeGrunnerTilReduksjon = false,
                    tilbakekrevSmåbeløp = true,
                    særligeGrunnerBegrunnelse = null,
                ),
            )
        }

        override fun tilEntity(): AktsomhetsvurderingEntity {
            return AktsomhetsvurderingEntity(
                vurderingType = VurderingType.FORÅRSAKET_AV_BRUKER_FORSETT,
                begrunnelse = begrunnelse,
                beløpIBehold = null,
                aktsomhet = null,
            )
        }
    }
}
