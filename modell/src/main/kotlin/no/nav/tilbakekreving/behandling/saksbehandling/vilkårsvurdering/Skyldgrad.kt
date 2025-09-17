package no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering

import no.nav.tilbakekreving.api.v1.dto.VurdertAktsomhetDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsresultatDto
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.endring.VurdertUtbetaling
import no.nav.tilbakekreving.entities.AktsomhetType
import no.nav.tilbakekreving.entities.AktsomhetsvurderingEntity
import no.nav.tilbakekreving.entities.FeilaktigEllerMangelfullType
import no.nav.tilbakekreving.entities.VurderingType
import no.nav.tilbakekreving.entities.VurdertAktsomhetEntity
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat

// §22-15 1. ledd 2. punktum (Før utbetaling)
sealed interface Skyldgrad : ForårsaketAvBruker.Ja {
    val feilaktigeEllerMangelfulleOpplysninger: FeilaktigEllerMangelfull
    val begrunnelseAktsomhet: String

    class Uaktsomt(
        override val begrunnelse: String,
        override val begrunnelseAktsomhet: String,
        private val kanUnnlates4XRettsgebyr: KanUnnlates4xRettsgebyr,
        override val feilaktigeEllerMangelfulleOpplysninger: FeilaktigEllerMangelfull,
    ) : Skyldgrad {
        override fun renter() = false

        override fun reduksjon(): Reduksjon = kanUnnlates4XRettsgebyr.reduksjon()

        override fun vurderingstype(): Aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET

        override fun oppsummerVurdering(): VurdertUtbetaling.Vilkårsvurdering {
            return VurdertUtbetaling.Vilkårsvurdering(
                aktsomhetFørUtbetaling = vurderingstype(),
                aktsomhetEtterUtbetaling = null,
                forårsaketAvBruker = when (feilaktigeEllerMangelfulleOpplysninger) {
                    FeilaktigEllerMangelfull.FEILAKTIG -> VurdertUtbetaling.ForårsaketAvBruker.FEILAKTIGE_OPPLYSNINGER
                    FeilaktigEllerMangelfull.MANGELFULL -> VurdertUtbetaling.ForårsaketAvBruker.MANGELFULLE_OPPLYSNINGER
                },
                særligeGrunner = when (kanUnnlates4XRettsgebyr) {
                    is KanUnnlates4xRettsgebyr.ErOver4xRettsgebyr -> kanUnnlates4XRettsgebyr.reduksjonSærligeGrunner.oppsummerVurdering()
                    else -> null
                },
                beløpUnnlatesUnder4Rettsgebyr = kanUnnlates4XRettsgebyr.oppsummering(),
            )
        }

        override fun tilFrontendDto(): VurdertVilkårsvurderingsresultatDto? {
            return VurdertVilkårsvurderingsresultatDto(
                vilkårsvurderingsresultat = feilaktigeEllerMangelfulleOpplysninger.vilkårsvurderingsresultat,
                aktsomhet = VurdertAktsomhetDto(
                    aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                    ileggRenter = renter(),
                    andelTilbakekreves = reduksjon().andel,
                    beløpTilbakekreves = null,
                    begrunnelse = begrunnelse,
                    særligeGrunner = when (kanUnnlates4XRettsgebyr) {
                        is KanUnnlates4xRettsgebyr.ErOver4xRettsgebyr -> kanUnnlates4XRettsgebyr.reduksjonSærligeGrunner.vurderteGrunner()
                        else -> null
                    },
                    særligeGrunnerTilReduksjon = when (kanUnnlates4XRettsgebyr) {
                        is KanUnnlates4xRettsgebyr.ErOver4xRettsgebyr -> kanUnnlates4XRettsgebyr.reduksjonSærligeGrunner.skalReduseres is ReduksjonSærligeGrunner.SkalReduseres.Ja
                        else -> false
                    },
                    tilbakekrevSmåbeløp = true,
                    særligeGrunnerBegrunnelse = when (kanUnnlates4XRettsgebyr) {
                        is KanUnnlates4xRettsgebyr.ErOver4xRettsgebyr -> kanUnnlates4XRettsgebyr.reduksjonSærligeGrunner.begrunnelse
                        else -> null
                    },
                ),
            )
        }

        override fun tilEntity(): AktsomhetsvurderingEntity {
            return AktsomhetsvurderingEntity(
                vurderingType = VurderingType.FORÅRSAKET_AV_BRUKER,
                begrunnelse = begrunnelse,
                beløpIBehold = null,
                aktsomhet = VurdertAktsomhetEntity(
                    aktsomhetType = AktsomhetType.SIMPEL_UAKTSOMHET,
                    begrunnelse = begrunnelseAktsomhet,
                    skalIleggesRenter = null,
                    særligGrunner = when (kanUnnlates4XRettsgebyr) {
                        is KanUnnlates4xRettsgebyr.ErOver4xRettsgebyr -> kanUnnlates4XRettsgebyr.reduksjonSærligeGrunner.tilEntity()
                        else -> null
                    },
                    kanUnnlates = kanUnnlates4XRettsgebyr.kanUnnlates,
                ),
                feilaktigeEllerMangelfulleOpplysninger.tilEntity(),
            )
        }
    }

    class GrovUaktsomhet(
        override val begrunnelse: String,
        override val begrunnelseAktsomhet: String,
        private val reduksjonSærligeGrunner: ReduksjonSærligeGrunner,
        override val feilaktigeEllerMangelfulleOpplysninger: FeilaktigEllerMangelfull,
    ) : Skyldgrad {
        override fun vurderingstype(): Aktsomhet = Aktsomhet.GROV_UAKTSOMHET

        override fun renter() = true

        override fun reduksjon(): Reduksjon = reduksjonSærligeGrunner.skalReduseres.reduksjon()

        override fun oppsummerVurdering(): VurdertUtbetaling.Vilkårsvurdering {
            return VurdertUtbetaling.Vilkårsvurdering(
                aktsomhetFørUtbetaling = vurderingstype(),
                aktsomhetEtterUtbetaling = null,
                forårsaketAvBruker = when (feilaktigeEllerMangelfulleOpplysninger) {
                    FeilaktigEllerMangelfull.FEILAKTIG -> VurdertUtbetaling.ForårsaketAvBruker.FEILAKTIGE_OPPLYSNINGER
                    FeilaktigEllerMangelfull.MANGELFULL -> VurdertUtbetaling.ForårsaketAvBruker.MANGELFULLE_OPPLYSNINGER
                },
                særligeGrunner = reduksjonSærligeGrunner.oppsummerVurdering(),
                beløpUnnlatesUnder4Rettsgebyr = VurdertUtbetaling.JaNeiVurdering.Nei,
            )
        }

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
                vurderingType = VurderingType.FORÅRSAKET_AV_BRUKER,
                begrunnelse = begrunnelse,
                beløpIBehold = null,
                aktsomhet = VurdertAktsomhetEntity(
                    aktsomhetType = AktsomhetType.GROV_UAKTSOMHET,
                    særligGrunner = reduksjonSærligeGrunner.tilEntity(),
                    begrunnelse = begrunnelseAktsomhet,
                    skalIleggesRenter = null,
                    kanUnnlates = KanUnnlates4xRettsgebyr.KanUnnlates.Nei,
                ),
                feilaktigEllerMangelfull = feilaktigeEllerMangelfulleOpplysninger.tilEntity(),
            )
        }
    }

    class Forsett(
        override val begrunnelse: String,
        override val begrunnelseAktsomhet: String,
        override val feilaktigeEllerMangelfulleOpplysninger: FeilaktigEllerMangelfull,
    ) : Skyldgrad {
        override fun renter() = true

        override fun vurderingstype(): Aktsomhet = Aktsomhet.FORSETT

        override fun reduksjon(): Reduksjon = Reduksjon.FullstendigTilbakekreving()

        override fun oppsummerVurdering(): VurdertUtbetaling.Vilkårsvurdering {
            return VurdertUtbetaling.Vilkårsvurdering(
                aktsomhetFørUtbetaling = vurderingstype(),
                aktsomhetEtterUtbetaling = null,
                forårsaketAvBruker = when (feilaktigeEllerMangelfulleOpplysninger) {
                    FeilaktigEllerMangelfull.FEILAKTIG -> VurdertUtbetaling.ForårsaketAvBruker.FEILAKTIGE_OPPLYSNINGER
                    FeilaktigEllerMangelfull.MANGELFULL -> VurdertUtbetaling.ForårsaketAvBruker.MANGELFULLE_OPPLYSNINGER
                },
                særligeGrunner = null,
                beløpUnnlatesUnder4Rettsgebyr = VurdertUtbetaling.JaNeiVurdering.Nei,
            )
        }

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
                vurderingType = VurderingType.FORÅRSAKET_AV_BRUKER,
                begrunnelse = begrunnelse,
                beløpIBehold = null,
                aktsomhet = VurdertAktsomhetEntity(
                    aktsomhetType = AktsomhetType.FORSETT,
                    begrunnelse = begrunnelseAktsomhet,
                    skalIleggesRenter = null,
                    særligGrunner = null,
                    kanUnnlates = KanUnnlates4xRettsgebyr.KanUnnlates.Nei,
                ),
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
