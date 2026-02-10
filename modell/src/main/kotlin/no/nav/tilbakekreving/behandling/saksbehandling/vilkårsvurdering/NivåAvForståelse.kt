package no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering

import no.nav.tilbakekreving.api.v1.dto.VurdertAktsomhetDto
import no.nav.tilbakekreving.api.v1.dto.VurdertGodTroDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsresultatDto
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.breeeev.PåkrevdBegrunnelse
import no.nav.tilbakekreving.endring.VurdertUtbetaling
import no.nav.tilbakekreving.entities.AktsomhetType
import no.nav.tilbakekreving.entities.AktsomhetsvurderingEntity
import no.nav.tilbakekreving.entities.BeholdType
import no.nav.tilbakekreving.entities.GodTroEntity
import no.nav.tilbakekreving.entities.VurderingType
import no.nav.tilbakekreving.entities.VurdertAktsomhetEntity
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.AnnenVurdering
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vurdering
import java.math.BigDecimal
import java.util.UUID
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet as AktsomhetDTO

interface NivåAvForståelse : ForårsaketAvBruker.Nei {
    class Forstod(
        val aktsomhet: Aktsomhet,
        override val begrunnelse: String,
    ) : NivåAvForståelse {
        override fun vurderingstype(): Vurdering = aktsomhet.vurderingstype()

        override fun reduksjon(): Reduksjon = aktsomhet.reduksjon()

        override fun renter(): Boolean = aktsomhet.renter()

        override fun tilFrontendDto(): VurdertVilkårsvurderingsresultatDto? {
            return VurdertVilkårsvurderingsresultatDto(
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                godTro = null,
                aktsomhet = aktsomhet.tilFrontendDto(),
            )
        }

        override fun oppsummerVurdering(): VurdertUtbetaling.Vilkårsvurdering {
            return VurdertUtbetaling.Vilkårsvurdering(
                aktsomhetFørUtbetaling = null,
                aktsomhetEtterUtbetaling = aktsomhet.vurderingstype(),
                forårsaketAvBruker = VurdertUtbetaling.ForårsaketAvBruker.IKKE_FORÅRSAKET_AV_BRUKER,
                særligeGrunner = aktsomhet.oppsummerSærligeGrunnerVurdering(),
                beløpUnnlatesUnder4Rettsgebyr = VurdertUtbetaling.JaNeiVurdering.Nei,
            )
        }

        override fun påkrevdeVurderinger(): Set<PåkrevdBegrunnelse> {
            return aktsomhet.påkrevdeVurderinger()
        }

        override fun tilEntity(periodeRef: UUID): AktsomhetsvurderingEntity {
            return AktsomhetsvurderingEntity(
                vurderingType = VurderingType.IKKE_FORÅRSAKET_AV_BRUKER_FORSTOD,
                beløpIBehold = null,
                begrunnelse = begrunnelse,
                aktsomhet = aktsomhet.tilEntity(periodeRef),
                feilaktigEllerMangelfull = null,
            )
        }
    }

    class BurdeForstått(
        val aktsomhet: Aktsomhet,
        override val begrunnelse: String,
    ) : NivåAvForståelse {
        override fun vurderingstype(): Vurdering = aktsomhet.vurderingstype()

        override fun reduksjon(): Reduksjon = aktsomhet.reduksjon()

        override fun renter(): Boolean = false

        override fun tilFrontendDto(): VurdertVilkårsvurderingsresultatDto? {
            return VurdertVilkårsvurderingsresultatDto(
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                godTro = null,
                aktsomhet = aktsomhet.tilFrontendDto(),
            )
        }

        override fun oppsummerVurdering(): VurdertUtbetaling.Vilkårsvurdering {
            return VurdertUtbetaling.Vilkårsvurdering(
                aktsomhetFørUtbetaling = null,
                aktsomhetEtterUtbetaling = aktsomhet.vurderingstype(),
                forårsaketAvBruker = VurdertUtbetaling.ForårsaketAvBruker.IKKE_FORÅRSAKET_AV_BRUKER,
                særligeGrunner = aktsomhet.oppsummerSærligeGrunnerVurdering(),
                beløpUnnlatesUnder4Rettsgebyr = aktsomhet.oppsummer4RettsgebyrVurdering(),
            )
        }

        override fun påkrevdeVurderinger(): Set<PåkrevdBegrunnelse> {
            return aktsomhet.påkrevdeVurderinger()
        }

        override fun tilEntity(periodeRef: UUID): AktsomhetsvurderingEntity {
            return AktsomhetsvurderingEntity(
                vurderingType = VurderingType.IKKE_FORÅRSAKET_AV_BRUKER_BURDE_FORSTÅTT,
                beløpIBehold = null,
                begrunnelse = begrunnelse,
                aktsomhet = aktsomhet.tilEntity(periodeRef),
                feilaktigEllerMangelfull = null,
            )
        }
    }

    class GodTro(
        private val beløpIBehold: BeløpIBehold,
        override val begrunnelse: String,
        val begrunnelseForGodTro: String,
    ) : NivåAvForståelse {
        override fun vurderingstype(): Vurdering = AnnenVurdering.GOD_TRO

        override fun reduksjon(): Reduksjon = beløpIBehold.reduksjon()

        override fun renter(): Boolean = false

        override fun tilFrontendDto(): VurdertVilkårsvurderingsresultatDto? {
            return VurdertVilkårsvurderingsresultatDto(
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.GOD_TRO,
                godTro = VurdertGodTroDto(
                    beløpErIBehold = beløpIBehold is BeløpIBehold.Ja,
                    beløpTilbakekreves = (beløpIBehold as? BeløpIBehold.Ja)?.beløp ?: BigDecimal.ZERO,
                    begrunnelse = begrunnelseForGodTro,
                ),
                aktsomhet = null,
            )
        }

        override fun oppsummerVurdering(): VurdertUtbetaling.Vilkårsvurdering {
            return VurdertUtbetaling.Vilkårsvurdering(
                aktsomhetFørUtbetaling = null,
                aktsomhetEtterUtbetaling = null,
                forårsaketAvBruker = VurdertUtbetaling.ForårsaketAvBruker.GOD_TRO,
                særligeGrunner = null,
                beløpUnnlatesUnder4Rettsgebyr = VurdertUtbetaling.JaNeiVurdering.Nei,
            )
        }

        override fun påkrevdeVurderinger(): Set<PåkrevdBegrunnelse> = emptySet()

        override fun tilEntity(periodeRef: UUID): AktsomhetsvurderingEntity {
            return AktsomhetsvurderingEntity(
                vurderingType = VurderingType.IKKE_FORÅRSAKET_AV_BRUKER_GOD_TRO,
                begrunnelse = begrunnelse,
                beløpIBehold = beløpIBehold.tilEntity(periodeRef, begrunnelseForGodTro),
                aktsomhet = null,
                feilaktigEllerMangelfull = null,
            )
        }

        sealed interface BeløpIBehold {
            fun reduksjon(): Reduksjon

            fun tilEntity(periodeRef: UUID, begrunnelse: String): GodTroEntity

            class Ja(val beløp: BigDecimal) : BeløpIBehold {
                override fun reduksjon(): Reduksjon {
                    return Reduksjon.ManueltBeløp(beløp)
                }

                override fun tilEntity(periodeRef: UUID, begrunnelse: String): GodTroEntity {
                    return GodTroEntity(
                        periodeRef = periodeRef,
                        begrunnelse = begrunnelse,
                        beholdType = BeholdType.JA,
                        beløp = beløp,
                    )
                }
            }

            data object Nei : BeløpIBehold {
                override fun reduksjon(): Reduksjon {
                    return Reduksjon.IngenTilbakekreving()
                }

                override fun tilEntity(periodeRef: UUID, begrunnelse: String): GodTroEntity {
                    return GodTroEntity(
                        periodeRef = periodeRef,
                        begrunnelse = begrunnelse,
                        beholdType = BeholdType.NEI,
                        beløp = null,
                    )
                }
            }
        }
    }

    // §22-15 1. ledd 1. punktum (Etter utbetaling)
    sealed interface Aktsomhet {
        fun vurderingstype(): AktsomhetDTO

        val begrunnelse: String

        fun reduksjon(): Reduksjon

        fun tilFrontendDto(): VurdertAktsomhetDto

        fun tilEntity(periodeRef: UUID): VurdertAktsomhetEntity

        fun renter(): Boolean

        fun oppsummerSærligeGrunnerVurdering(): VurdertUtbetaling.SærligeGrunner?

        fun oppsummer4RettsgebyrVurdering(): VurdertUtbetaling.JaNeiVurdering

        fun påkrevdeVurderinger(): Set<PåkrevdBegrunnelse>

        class Forsett(
            override val begrunnelse: String,
        ) : Aktsomhet {
            override fun vurderingstype(): AktsomhetDTO = AktsomhetDTO.FORSETT

            override fun reduksjon(): Reduksjon = Reduksjon.FullstendigTilbakekreving()

            override fun renter(): Boolean = true

            override fun tilFrontendDto(): VurdertAktsomhetDto {
                return VurdertAktsomhetDto(
                    aktsomhet = vurderingstype(),
                    ileggRenter = false,
                    andelTilbakekreves = reduksjon().andel,
                    beløpTilbakekreves = null,
                    begrunnelse = begrunnelse,
                    særligeGrunner = null,
                    særligeGrunnerTilReduksjon = false,
                    tilbakekrevSmåbeløp = false,
                    særligeGrunnerBegrunnelse = null,
                )
            }

            override fun oppsummerSærligeGrunnerVurdering(): VurdertUtbetaling.SærligeGrunner? {
                return null
            }

            override fun oppsummer4RettsgebyrVurdering(): VurdertUtbetaling.JaNeiVurdering {
                return VurdertUtbetaling.JaNeiVurdering.Nei
            }

            override fun påkrevdeVurderinger(): Set<PåkrevdBegrunnelse> = emptySet()

            override fun tilEntity(periodeRef: UUID): VurdertAktsomhetEntity {
                return VurdertAktsomhetEntity(
                    periodeRef = periodeRef,
                    aktsomhetType = AktsomhetType.FORSETT,
                    begrunnelse = begrunnelse,
                    skalIleggesRenter = null,
                    særligGrunner = null,
                    kanUnnlates = null,
                )
            }
        }

        class GrovUaktsomhet(
            private val reduksjonSærligeGrunner: ReduksjonSærligeGrunner,
            override val begrunnelse: String,
        ) : Aktsomhet {
            override fun reduksjon(): Reduksjon {
                return reduksjonSærligeGrunner.skalReduseres.reduksjon()
            }

            override fun renter(): Boolean = true

            override fun vurderingstype(): AktsomhetDTO = AktsomhetDTO.GROV_UAKTSOMHET

            override fun tilFrontendDto(): VurdertAktsomhetDto {
                return VurdertAktsomhetDto(
                    aktsomhet = vurderingstype(),
                    ileggRenter = false,
                    andelTilbakekreves = reduksjon().andel,
                    beløpTilbakekreves = null,
                    begrunnelse = begrunnelse,
                    særligeGrunner = reduksjonSærligeGrunner.vurderteGrunner(),
                    særligeGrunnerTilReduksjon = reduksjonSærligeGrunner.skalReduseres is ReduksjonSærligeGrunner.SkalReduseres.Ja,
                    tilbakekrevSmåbeløp = true,
                    særligeGrunnerBegrunnelse = reduksjonSærligeGrunner.begrunnelse,
                )
            }

            override fun oppsummerSærligeGrunnerVurdering(): VurdertUtbetaling.SærligeGrunner? {
                return reduksjonSærligeGrunner.oppsummerVurdering()
            }

            override fun oppsummer4RettsgebyrVurdering(): VurdertUtbetaling.JaNeiVurdering {
                return VurdertUtbetaling.JaNeiVurdering.Nei
            }

            override fun påkrevdeVurderinger(): Set<PåkrevdBegrunnelse> = emptySet()

            override fun tilEntity(periodeRef: UUID): VurdertAktsomhetEntity {
                return VurdertAktsomhetEntity(
                    periodeRef = periodeRef,
                    aktsomhetType = AktsomhetType.GROV_UAKTSOMHET,
                    begrunnelse = begrunnelse,
                    skalIleggesRenter = null,
                    særligGrunner = reduksjonSærligeGrunner.tilEntity(periodeRef),
                    kanUnnlates = null,
                )
            }
        }

        class IkkeUtvistSkyld(
            private val kanUnnlates4XRettsgebyr: KanUnnlates4xRettsgebyr,
            override val begrunnelse: String,
        ) : Aktsomhet {
            override fun reduksjon(): Reduksjon = kanUnnlates4XRettsgebyr.reduksjon()

            override fun renter(): Boolean = false

            override fun vurderingstype(): AktsomhetDTO = AktsomhetDTO.SIMPEL_UAKTSOMHET

            override fun oppsummerSærligeGrunnerVurdering(): VurdertUtbetaling.SærligeGrunner? {
                return (kanUnnlates4XRettsgebyr as? KanUnnlates4xRettsgebyr.SkalIkkeUnnlates)?.reduksjonSærligeGrunner?.oppsummerVurdering()
            }

            override fun oppsummer4RettsgebyrVurdering(): VurdertUtbetaling.JaNeiVurdering = when (kanUnnlates4XRettsgebyr) {
                is KanUnnlates4xRettsgebyr.ErOver4xRettsgebyr, is KanUnnlates4xRettsgebyr.SkalIkkeUnnlates -> VurdertUtbetaling.JaNeiVurdering.Nei
                is KanUnnlates4xRettsgebyr.Unnlates -> VurdertUtbetaling.JaNeiVurdering.Ja
            }

            override fun påkrevdeVurderinger(): Set<PåkrevdBegrunnelse> = emptySet()

            override fun tilFrontendDto(): VurdertAktsomhetDto {
                return VurdertAktsomhetDto(
                    aktsomhet = vurderingstype(),
                    ileggRenter = false,
                    andelTilbakekreves = reduksjon().andel,
                    beløpTilbakekreves = null,
                    begrunnelse = begrunnelse,
                    særligeGrunner = (kanUnnlates4XRettsgebyr as? KanUnnlates4xRettsgebyr.SkalIkkeUnnlates)?.reduksjonSærligeGrunner?.vurderteGrunner(),
                    særligeGrunnerTilReduksjon = (kanUnnlates4XRettsgebyr as? KanUnnlates4xRettsgebyr.SkalIkkeUnnlates)?.reduksjonSærligeGrunner?.skalReduseres is ReduksjonSærligeGrunner.SkalReduseres.Ja,
                    tilbakekrevSmåbeløp = kanUnnlates4XRettsgebyr is KanUnnlates4xRettsgebyr.SkalIkkeUnnlates,
                    særligeGrunnerBegrunnelse = (kanUnnlates4XRettsgebyr as? KanUnnlates4xRettsgebyr.SkalIkkeUnnlates)?.reduksjonSærligeGrunner?.begrunnelse,
                )
            }

            override fun tilEntity(periodeRef: UUID): VurdertAktsomhetEntity {
                return VurdertAktsomhetEntity(
                    periodeRef = periodeRef,
                    aktsomhetType = AktsomhetType.IKKE_UTVIST_SKYLD,
                    begrunnelse = begrunnelse,
                    skalIleggesRenter = null,
                    særligGrunner = (kanUnnlates4XRettsgebyr as? KanUnnlates4xRettsgebyr.SkalIkkeUnnlates)?.reduksjonSærligeGrunner?.tilEntity(periodeRef),
                    kanUnnlates = kanUnnlates4XRettsgebyr.tilEntity(),
                )
            }
        }

        class Uaktsomhet(
            private val kanUnnlates4XRettsgebyr: KanUnnlates4xRettsgebyr,
            override val begrunnelse: String,
        ) : Aktsomhet {
            override fun reduksjon(): Reduksjon = kanUnnlates4XRettsgebyr.reduksjon()

            override fun renter(): Boolean = false

            override fun vurderingstype(): AktsomhetDTO = AktsomhetDTO.SIMPEL_UAKTSOMHET

            override fun oppsummerSærligeGrunnerVurdering(): VurdertUtbetaling.SærligeGrunner? {
                return (kanUnnlates4XRettsgebyr as? KanUnnlates4xRettsgebyr.SkalIkkeUnnlates)?.reduksjonSærligeGrunner?.oppsummerVurdering()
            }

            override fun oppsummer4RettsgebyrVurdering(): VurdertUtbetaling.JaNeiVurdering = when (kanUnnlates4XRettsgebyr) {
                is KanUnnlates4xRettsgebyr.ErOver4xRettsgebyr, is KanUnnlates4xRettsgebyr.SkalIkkeUnnlates -> VurdertUtbetaling.JaNeiVurdering.Nei
                is KanUnnlates4xRettsgebyr.Unnlates -> VurdertUtbetaling.JaNeiVurdering.Ja
            }

            override fun påkrevdeVurderinger(): Set<PåkrevdBegrunnelse> = emptySet()

            override fun tilFrontendDto(): VurdertAktsomhetDto {
                return VurdertAktsomhetDto(
                    aktsomhet = vurderingstype(),
                    ileggRenter = false,
                    andelTilbakekreves = reduksjon().andel,
                    beløpTilbakekreves = null,
                    begrunnelse = begrunnelse,
                    særligeGrunner = (kanUnnlates4XRettsgebyr as? KanUnnlates4xRettsgebyr.SkalIkkeUnnlates)?.reduksjonSærligeGrunner?.vurderteGrunner(),
                    særligeGrunnerTilReduksjon = (kanUnnlates4XRettsgebyr as? KanUnnlates4xRettsgebyr.SkalIkkeUnnlates)?.reduksjonSærligeGrunner?.skalReduseres is ReduksjonSærligeGrunner.SkalReduseres.Ja,
                    tilbakekrevSmåbeløp = kanUnnlates4XRettsgebyr is KanUnnlates4xRettsgebyr.SkalIkkeUnnlates,
                    særligeGrunnerBegrunnelse = (kanUnnlates4XRettsgebyr as? KanUnnlates4xRettsgebyr.SkalIkkeUnnlates)?.reduksjonSærligeGrunner?.begrunnelse,
                )
            }

            override fun tilEntity(periodeRef: UUID): VurdertAktsomhetEntity {
                return VurdertAktsomhetEntity(
                    periodeRef = periodeRef,
                    aktsomhetType = AktsomhetType.SIMPEL_UAKTSOMHET,
                    begrunnelse = begrunnelse,
                    skalIleggesRenter = null,
                    særligGrunner = (kanUnnlates4XRettsgebyr as? KanUnnlates4xRettsgebyr.SkalIkkeUnnlates)?.reduksjonSærligeGrunner?.tilEntity(periodeRef),
                    kanUnnlates = kanUnnlates4XRettsgebyr.tilEntity(),
                )
            }
        }
    }
}
