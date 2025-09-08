package no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering

import no.nav.tilbakekreving.api.v1.dto.VurdertAktsomhetDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsresultatDto
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.entities.AktsomhetsvurderingEntity
import no.nav.tilbakekreving.entities.FeilaktigEllerMangelfullType
import no.nav.tilbakekreving.entities.VurderingType
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vurdering

sealed interface Skyldgrad : ForårsaketAvBruker.Ja {
    val feilaktigeEllerMangelfulleOpplysninger: FeilaktigEllerMangelfull

    class Uaktsomt(
        override val begrunnelse: String,
        private val reduksjonSærligeGrunner: ReduksjonSærligeGrunner,
        override val feilaktigeEllerMangelfulleOpplysninger: FeilaktigEllerMangelfull,
    ) : Skyldgrad {
        override fun renter() = false

        override fun reduksjon(): Reduksjon = reduksjonSærligeGrunner.skalReduseres.reduksjon()

        override fun vurderingstype(): Vurdering = Aktsomhet.SIMPEL_UAKTSOMHET

        override fun tilFrontendDto(): VurdertVilkårsvurderingsresultatDto? {
            return VurdertVilkårsvurderingsresultatDto(
                vilkårsvurderingsresultat = feilaktigeEllerMangelfulleOpplysninger.vilkårsvurderingsresultat,
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
                vurderingType = VurderingType.FORÅRSAKET_AV_BRUKER_UAKTSOMT,
                begrunnelse = begrunnelse,
                beløpIBehold = null,
                aktsomhet = null,
                feilaktigeEllerMangelfulleOpplysninger.tilEntity(),
            )
        }
    }

    class GrovUaktsomhet(
        override val begrunnelse: String,
        private val reduksjonSærligeGrunner: ReduksjonSærligeGrunner,
        override val feilaktigeEllerMangelfulleOpplysninger: FeilaktigEllerMangelfull,
    ) : Skyldgrad {
        override fun vurderingstype(): Vurdering = Aktsomhet.GROV_UAKTSOMHET

        override fun renter() = true

        override fun reduksjon(): Reduksjon = reduksjonSærligeGrunner.skalReduseres.reduksjon()

        override fun tilFrontendDto(): VurdertVilkårsvurderingsresultatDto? {
            return VurdertVilkårsvurderingsresultatDto(
                vilkårsvurderingsresultat = feilaktigeEllerMangelfulleOpplysninger.vilkårsvurderingsresultat,
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
                feilaktigEllerMangelfull = feilaktigeEllerMangelfulleOpplysninger.tilEntity(),
            )
        }
    }

    class Forsett(
        override val begrunnelse: String,
        override val feilaktigeEllerMangelfulleOpplysninger: FeilaktigEllerMangelfull,
    ) : Skyldgrad {
        override fun renter() = true

        override fun vurderingstype(): Vurdering = Aktsomhet.FORSETT

        override fun reduksjon(): Reduksjon = Reduksjon.FullstendigTilbakekreving()

        override fun tilFrontendDto(): VurdertVilkårsvurderingsresultatDto? {
            return VurdertVilkårsvurderingsresultatDto(
                vilkårsvurderingsresultat = feilaktigeEllerMangelfulleOpplysninger.vilkårsvurderingsresultat,
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
                feilaktigEllerMangelfull = feilaktigeEllerMangelfulleOpplysninger.tilEntity(),
            )
        }
    }

    enum class FeilaktigEllerMangelfull(val vilkårsvurderingsresultat: Vilkårsvurderingsresultat) {
        FEILAKTIG(Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER),
        MANGELFULL(Vilkårsvurderingsresultat.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER),
        ;

        fun tilEntity() = FeilaktigEllerMangelfullType.entries.single { it.fraEntity == this }
    }
}
