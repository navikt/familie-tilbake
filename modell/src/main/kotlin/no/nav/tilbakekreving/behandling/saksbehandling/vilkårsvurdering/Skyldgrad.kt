package no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering

import no.nav.tilbakekreving.api.v1.dto.SkalUnnlates
import no.nav.tilbakekreving.api.v1.dto.VurdertAktsomhetDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsresultatDto
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.breeeev.begrunnelse.VilkårsvurderingBegrunnelse
import no.nav.tilbakekreving.endring.VurdertUtbetaling
import no.nav.tilbakekreving.entities.AktsomhetType
import no.nav.tilbakekreving.entities.AktsomhetsvurderingEntity
import no.nav.tilbakekreving.entities.FeilaktigEllerMangelfullType
import no.nav.tilbakekreving.entities.VurderingType
import no.nav.tilbakekreving.entities.VurdertAktsomhetEntity
import no.nav.tilbakekreving.kontrakter.frontend.models.ForaarsaketAvMottakerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForsettligDto
import no.nav.tilbakekreving.kontrakter.frontend.models.GrovtUaktsomtDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UaktsomtDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VilkaarsvurderingValgDto
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import java.util.UUID

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

        override fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse> = kanUnnlates4XRettsgebyr.påkrevdeVurderinger()

        override fun oppsummerVurdering(): VurdertUtbetaling.Vilkårsvurdering {
            val reduksjonSærligeGrunner = kanUnnlates4XRettsgebyr.reduksjonMomenter() as? ReduksjonMomenter.ReduksjonSærligeGrunner
            return VurdertUtbetaling.Vilkårsvurdering(
                aktsomhetFørUtbetaling = vurderingstype(),
                aktsomhetEtterUtbetaling = null,
                forårsaketAvBruker = when (feilaktigeEllerMangelfulleOpplysninger) {
                    FeilaktigEllerMangelfull.FEILAKTIG -> VurdertUtbetaling.ForårsaketAvBruker.FEILAKTIGE_OPPLYSNINGER
                    FeilaktigEllerMangelfull.MANGELFULL -> VurdertUtbetaling.ForårsaketAvBruker.MANGELFULLE_OPPLYSNINGER
                    FeilaktigEllerMangelfull.IKKE_VURDERT -> throw IllegalArgumentException("Ikke_Vurdert er kun til ny vilkårsvurdering")
                },
                særligeGrunner = reduksjonSærligeGrunner?.oppsummerVurdering(),
                beløpUnnlatesUnder4Rettsgebyr = kanUnnlates4XRettsgebyr.oppsummering(),
            )
        }

        override fun tilNyFrontendDto(): VilkaarsvurderingValgDto {
            return ForaarsaketAvMottakerDto(
                aktsomhet = UaktsomtDto(
                    begrunnelse = begrunnelseAktsomhet,
                    unnlatelse = kanUnnlates4XRettsgebyr.tilFrontendDto(),
                ),
            )
        }

        override fun tilFrontendDto(): VurdertVilkårsvurderingsresultatDto? {
            val reduksjonSærligeGrunner = kanUnnlates4XRettsgebyr.reduksjonMomenter() as? ReduksjonMomenter.ReduksjonSærligeGrunner
            return VurdertVilkårsvurderingsresultatDto(
                vilkårsvurderingsresultat = feilaktigeEllerMangelfulleOpplysninger.vilkårsvurderingsresultat,
                aktsomhet = VurdertAktsomhetDto(
                    aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                    ileggRenter = renter(),
                    andelTilbakekreves = reduksjon().andel,
                    beløpTilbakekreves = null,
                    begrunnelse = begrunnelseAktsomhet,
                    særligeGrunner = reduksjonSærligeGrunner?.vurderteGrunner(),
                    særligeGrunnerTilReduksjon = reduksjonSærligeGrunner?.skalReduseres is ReduksjonMomenter.SkalReduseres.Ja,
                    tilbakekrevSmåbeløp = kanUnnlates4XRettsgebyr.skalTilbakekreves(),
                    unnlates4Rettsgebyr = kanUnnlates4XRettsgebyr.tilFrontendDTO(),
                    særligeGrunnerBegrunnelse = reduksjonSærligeGrunner?.begrunnelse,
                ),
            )
        }

        override fun tilEntity(periodeRef: UUID): AktsomhetsvurderingEntity {
            val reduksjonSærligeGrunner = kanUnnlates4XRettsgebyr.reduksjonMomenter() as? ReduksjonMomenter.ReduksjonSærligeGrunner
            return AktsomhetsvurderingEntity(
                vurderingType = VurderingType.FORÅRSAKET_AV_BRUKER,
                mottakersForståelse = null,
                begrunnelse = begrunnelse,
                beløpIBehold = null,
                aktsomhet = VurdertAktsomhetEntity(
                    periodeRef = periodeRef,
                    aktsomhetType = AktsomhetType.SIMPEL_UAKTSOMHET,
                    begrunnelse = begrunnelseAktsomhet,
                    skalIleggesRenter = null,
                ),
                begrunnelseForUnnlatelse = kanUnnlates4XRettsgebyr.begrunnelseForUnnlatelse(),
                kanUnnlates = kanUnnlates4XRettsgebyr.tilEntity(),
                reduksjonMomenterEntity = reduksjonSærligeGrunner?.tilEntity(periodeRef),
                feilaktigEllerMangelfull = feilaktigeEllerMangelfulleOpplysninger.tilEntity(),
                forrigePeriodeId = null,
            )
        }
    }

    class GrovUaktsomhet(
        override val begrunnelse: String,
        override val begrunnelseAktsomhet: String,
        private val reduksjonSærligeGrunner: ReduksjonMomenter.ReduksjonSærligeGrunner,
        override val feilaktigeEllerMangelfulleOpplysninger: FeilaktigEllerMangelfull,
    ) : Skyldgrad {
        override fun vurderingstype(): Aktsomhet = Aktsomhet.GROV_UAKTSOMHET

        override fun renter() = true

        override fun reduksjon(): Reduksjon = reduksjonSærligeGrunner.skalReduseres.reduksjon()

        override fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse> = setOf(VilkårsvurderingBegrunnelse.TILBAKEKREVES) + reduksjonSærligeGrunner.skalReduseres.påkrevdeVurderinger()

        override fun oppsummerVurdering(): VurdertUtbetaling.Vilkårsvurdering {
            return VurdertUtbetaling.Vilkårsvurdering(
                aktsomhetFørUtbetaling = vurderingstype(),
                aktsomhetEtterUtbetaling = null,
                forårsaketAvBruker = when (feilaktigeEllerMangelfulleOpplysninger) {
                    FeilaktigEllerMangelfull.FEILAKTIG -> VurdertUtbetaling.ForårsaketAvBruker.FEILAKTIGE_OPPLYSNINGER
                    FeilaktigEllerMangelfull.MANGELFULL -> VurdertUtbetaling.ForårsaketAvBruker.MANGELFULLE_OPPLYSNINGER
                    FeilaktigEllerMangelfull.IKKE_VURDERT -> throw IllegalArgumentException("Ikke_Vurdert er kun til ny vilkårsvurdering")
                },
                særligeGrunner = reduksjonSærligeGrunner.oppsummerVurdering(),
                beløpUnnlatesUnder4Rettsgebyr = VurdertUtbetaling.JaNeiVurdering.Nei,
            )
        }

        override fun tilNyFrontendDto(): VilkaarsvurderingValgDto {
            return ForaarsaketAvMottakerDto(
                aktsomhet = GrovtUaktsomtDto(
                    begrunnelse = begrunnelseAktsomhet,
                    erDetSærligeGrunner = reduksjonSærligeGrunner.tilFrontendDto(),
                ),
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
                    begrunnelse = begrunnelseAktsomhet,
                    særligeGrunner = reduksjonSærligeGrunner.vurderteGrunner(),
                    særligeGrunnerTilReduksjon = reduksjonSærligeGrunner.skalReduseres is ReduksjonMomenter.SkalReduseres.Ja,
                    tilbakekrevSmåbeløp = true,
                    unnlates4Rettsgebyr = SkalUnnlates.TILBAKEKREVES,
                    særligeGrunnerBegrunnelse = reduksjonSærligeGrunner.begrunnelse,
                ),
            )
        }

        override fun tilEntity(periodeRef: UUID): AktsomhetsvurderingEntity {
            return AktsomhetsvurderingEntity(
                vurderingType = VurderingType.FORÅRSAKET_AV_BRUKER,
                mottakersForståelse = null,
                begrunnelse = begrunnelse,
                beløpIBehold = null,
                aktsomhet = VurdertAktsomhetEntity(
                    periodeRef = periodeRef,
                    aktsomhetType = AktsomhetType.GROV_UAKTSOMHET,
                    begrunnelse = begrunnelseAktsomhet,
                    skalIleggesRenter = null,
                ),
                kanUnnlates = null,
                reduksjonMomenterEntity = reduksjonSærligeGrunner.tilEntity(periodeRef),
                feilaktigEllerMangelfull = feilaktigeEllerMangelfulleOpplysninger.tilEntity(),
                begrunnelseForUnnlatelse = null,
                forrigePeriodeId = null,
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

        override fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse> = setOf(VilkårsvurderingBegrunnelse.TILBAKEKREVES, VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER)

        override fun oppsummerVurdering(): VurdertUtbetaling.Vilkårsvurdering {
            return VurdertUtbetaling.Vilkårsvurdering(
                aktsomhetFørUtbetaling = vurderingstype(),
                aktsomhetEtterUtbetaling = null,
                forårsaketAvBruker = when (feilaktigeEllerMangelfulleOpplysninger) {
                    FeilaktigEllerMangelfull.FEILAKTIG -> VurdertUtbetaling.ForårsaketAvBruker.FEILAKTIGE_OPPLYSNINGER
                    FeilaktigEllerMangelfull.MANGELFULL -> VurdertUtbetaling.ForårsaketAvBruker.MANGELFULLE_OPPLYSNINGER
                    FeilaktigEllerMangelfull.IKKE_VURDERT -> throw IllegalArgumentException("Ikke_Vurdert er kun til ny vilkårsvurdering")
                },
                særligeGrunner = null,
                beløpUnnlatesUnder4Rettsgebyr = VurdertUtbetaling.JaNeiVurdering.Nei,
            )
        }

        override fun tilNyFrontendDto(): VilkaarsvurderingValgDto {
            return ForaarsaketAvMottakerDto(
                aktsomhet = ForsettligDto(
                    begrunnelse = begrunnelseAktsomhet,
                ),
            )
        }

        override fun tilFrontendDto(): VurdertVilkårsvurderingsresultatDto? {
            return VurdertVilkårsvurderingsresultatDto(
                vilkårsvurderingsresultat = feilaktigeEllerMangelfulleOpplysninger.vilkårsvurderingsresultat,
                aktsomhet = VurdertAktsomhetDto(
                    aktsomhet = Aktsomhet.FORSETT,
                    ileggRenter = renter(),
                    andelTilbakekreves = reduksjon().andel,
                    beløpTilbakekreves = null,
                    begrunnelse = begrunnelseAktsomhet,
                    særligeGrunner = null,
                    særligeGrunnerTilReduksjon = false,
                    tilbakekrevSmåbeløp = true,
                    unnlates4Rettsgebyr = SkalUnnlates.TILBAKEKREVES,
                    særligeGrunnerBegrunnelse = null,
                ),
            )
        }

        override fun tilEntity(periodeRef: UUID): AktsomhetsvurderingEntity {
            return AktsomhetsvurderingEntity(
                vurderingType = VurderingType.FORÅRSAKET_AV_BRUKER,
                mottakersForståelse = null,
                begrunnelse = begrunnelse,
                beløpIBehold = null,
                aktsomhet = VurdertAktsomhetEntity(
                    periodeRef = periodeRef,
                    aktsomhetType = AktsomhetType.FORSETT,
                    begrunnelse = begrunnelseAktsomhet,
                    skalIleggesRenter = null,
                ),
                kanUnnlates = null,
                reduksjonMomenterEntity = null,
                feilaktigEllerMangelfull = feilaktigeEllerMangelfulleOpplysninger.tilEntity(),
                begrunnelseForUnnlatelse = null,
                forrigePeriodeId = null,
            )
        }
    }

    enum class FeilaktigEllerMangelfull(val vilkårsvurderingsresultat: Vilkårsvurderingsresultat) {
        FEILAKTIG(Vilkårsvurderingsresultat.FEIL_OPPLYSNINGER_FRA_BRUKER),
        MANGELFULL(Vilkårsvurderingsresultat.MANGELFULLE_OPPLYSNINGER_FRA_BRUKER),
        IKKE_VURDERT(Vilkårsvurderingsresultat.UDEFINERT),
        ;

        fun tilEntity() = FeilaktigEllerMangelfullType.entries.single { it.fraEntity == this }
    }
}
