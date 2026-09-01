package no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering

import no.nav.tilbakekreving.api.v1.dto.SkalUnnlates
import no.nav.tilbakekreving.api.v1.dto.VurdertAktsomhetDto
import no.nav.tilbakekreving.api.v1.dto.VurdertGodTroDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingsresultatDto
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.breeeev.begrunnelse.VilkårsvurderingBegrunnelse
import no.nav.tilbakekreving.endring.VurdertUtbetaling
import no.nav.tilbakekreving.entities.AktsomhetsvurderingEntity
import no.nav.tilbakekreving.entities.BeholdType
import no.nav.tilbakekreving.entities.Forståelsesgrad
import no.nav.tilbakekreving.entities.GodTroEntity
import no.nav.tilbakekreving.entities.MottakersForståelseEntity
import no.nav.tilbakekreving.entities.VurderingType
import no.nav.tilbakekreving.kontrakter.frontend.models.BelopIBeholdDto
import no.nav.tilbakekreving.kontrakter.frontend.models.BurdeForstaattDto
import no.nav.tilbakekreving.kontrakter.frontend.models.DelerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForstoDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForstoEllerBurdeForstaattDto
import no.nav.tilbakekreving.kontrakter.frontend.models.GodTroDto
import no.nav.tilbakekreving.kontrakter.frontend.models.HeleDto
import no.nav.tilbakekreving.kontrakter.frontend.models.IngentingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SkalIkkeReduseresDto
import no.nav.tilbakekreving.kontrakter.frontend.models.VilkaarsvurderingValgDto
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.AnnenVurdering
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vurdering
import java.math.BigDecimal
import java.util.UUID
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet as AktsomhetDTO

interface NivåAvForståelse : ForårsaketAvBruker.Nei {
    class Forstod(
        val begrunnelseMottakersForståelse: String,
        override val begrunnelse: String,
        private val kanUnnlates4XRettsgebyr: KanUnnlates4xRettsgebyr?,
    ) : NivåAvForståelse {
        override fun vurderingstype(): Vurdering = Type.Forstod

        override fun reduksjon(): Reduksjon = kanUnnlates4XRettsgebyr?.reduksjon() ?: Reduksjon.FullstendigTilbakekreving()

        override fun renter(): Boolean = false

        override fun tilNyFrontendDto(): VilkaarsvurderingValgDto {
            return ForstoEllerBurdeForstaattDto(
                forståelse = ForstoDto(
                    begrunnelse = begrunnelseMottakersForståelse,
                    unnlatelse = kanUnnlates4XRettsgebyr!!.tilFrontendDto(),
                ),
            )
        }

        override fun tilFrontendDto(): VurdertVilkårsvurderingsresultatDto {
            return VurdertVilkårsvurderingsresultatDto(
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                godTro = null,
                aktsomhet = VurdertAktsomhetDto(
                    aktsomhet = AktsomhetDTO.FORSETT,
                    ileggRenter = false,
                    andelTilbakekreves = reduksjon().andel,
                    beløpTilbakekreves = null,
                    begrunnelse = begrunnelseMottakersForståelse,
                    særligeGrunner = null,
                    særligeGrunnerTilReduksjon = false,
                    tilbakekrevSmåbeløp = true,
                    unnlates4Rettsgebyr = SkalUnnlates.TILBAKEKREVES,
                    særligeGrunnerBegrunnelse = null,
                ),
            )
        }

        override fun oppsummerVurdering(): VurdertUtbetaling.Vilkårsvurdering {
            return VurdertUtbetaling.Vilkårsvurdering(
                aktsomhetFørUtbetaling = null,
                aktsomhetEtterUtbetaling = AktsomhetDTO.FORSETT,
                forårsaketAvBruker = VurdertUtbetaling.ForårsaketAvBruker.IKKE_FORÅRSAKET_AV_BRUKER,
                særligeGrunner = null,
                beløpUnnlatesUnder4Rettsgebyr = VurdertUtbetaling.JaNeiVurdering.Nei,
            )
        }

        override fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse> = setOf(VilkårsvurderingBegrunnelse.TILBAKEKREVES)

        override fun tilEntity(periodeRef: UUID): AktsomhetsvurderingEntity {
            val reduksjonSærligeGrunner = kanUnnlates4XRettsgebyr?.reduksjonMomenter() as? ReduksjonMomenter.ReduksjonSærligeGrunner
            return AktsomhetsvurderingEntity(
                vurderingType = VurderingType.IKKE_FORÅRSAKET_AV_BRUKER_FORSTOD,
                mottakersForståelse = MottakersForståelseEntity(
                    periodeRef = periodeRef,
                    mottakersForståelse = Forståelsesgrad.FORSTOD,
                    begrunnelse = begrunnelseMottakersForståelse,
                ),
                beløpIBehold = null,
                begrunnelse = begrunnelse,
                aktsomhet = null,
                begrunnelseForUnnlatelse = kanUnnlates4XRettsgebyr?.begrunnelseForUnnlatelse(),
                kanUnnlates = kanUnnlates4XRettsgebyr?.tilEntity(),
                reduksjonMomenterEntity = reduksjonSærligeGrunner?.tilEntity(periodeRef),
                feilaktigEllerMangelfull = null,
                forrigePeriodeId = null,
            )
        }
    }

    class BurdeForstått(
        val grad: Grad,
        val begrunnelseMottakersForståelse: String,
        private val kanUnnlates4XRettsgebyr: KanUnnlates4xRettsgebyr,
        override val begrunnelse: String,
    ) : NivåAvForståelse {
        override fun vurderingstype(): Vurdering = when (grad) {
            Grad.BURDE_FORSTÅTT -> Type.BurdeForstått
            Grad.MÅTTE_FORSTÅ -> Type.MåForstått
        }

        override fun reduksjon(): Reduksjon = kanUnnlates4XRettsgebyr.reduksjon()

        override fun renter(): Boolean = false

        override fun tilFrontendDto(): VurdertVilkårsvurderingsresultatDto {
            val reduksjonSærligeGrunner = kanUnnlates4XRettsgebyr.reduksjonMomenter() as? ReduksjonMomenter.ReduksjonSærligeGrunner
            return VurdertVilkårsvurderingsresultatDto(
                vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                godTro = null,
                aktsomhet = VurdertAktsomhetDto(
                    aktsomhet = grad.aktsomhet,
                    ileggRenter = false,
                    andelTilbakekreves = kanUnnlates4XRettsgebyr.reduksjon().andel,
                    beløpTilbakekreves = null,
                    begrunnelse = begrunnelseMottakersForståelse,
                    særligeGrunner = reduksjonSærligeGrunner?.vurderteGrunner(),
                    særligeGrunnerTilReduksjon = reduksjonSærligeGrunner?.skalReduseres is ReduksjonMomenter.SkalReduseres.Ja,
                    tilbakekrevSmåbeløp = kanUnnlates4XRettsgebyr.skalTilbakekreves(),
                    unnlates4Rettsgebyr = kanUnnlates4XRettsgebyr.tilFrontendDTO(),
                    særligeGrunnerBegrunnelse = reduksjonSærligeGrunner?.begrunnelse,
                ),
            )
        }

        override fun tilNyFrontendDto(): VilkaarsvurderingValgDto =
            ForstoEllerBurdeForstaattDto(
                forståelse = BurdeForstaattDto(
                    begrunnelse = begrunnelseMottakersForståelse,
                    unnlatelse = kanUnnlates4XRettsgebyr.tilFrontendDto(),
                ),
            )

        override fun oppsummerVurdering(): VurdertUtbetaling.Vilkårsvurdering {
            val reduksjonSærligeGrunner = kanUnnlates4XRettsgebyr.reduksjonMomenter() as? ReduksjonMomenter.ReduksjonSærligeGrunner
            return VurdertUtbetaling.Vilkårsvurdering(
                aktsomhetFørUtbetaling = null,
                aktsomhetEtterUtbetaling = grad.aktsomhet,
                forårsaketAvBruker = VurdertUtbetaling.ForårsaketAvBruker.IKKE_FORÅRSAKET_AV_BRUKER,
                særligeGrunner = reduksjonSærligeGrunner?.oppsummerVurdering(),
                beløpUnnlatesUnder4Rettsgebyr = kanUnnlates4XRettsgebyr.oppsummering(),
            )
        }

        override fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse> = kanUnnlates4XRettsgebyr.påkrevdeVurderinger()

        override fun tilEntity(periodeRef: UUID): AktsomhetsvurderingEntity {
            val reduksjonSærligeGrunner = kanUnnlates4XRettsgebyr.reduksjonMomenter() as? ReduksjonMomenter.ReduksjonSærligeGrunner
            return AktsomhetsvurderingEntity(
                vurderingType = VurderingType.IKKE_FORÅRSAKET_AV_BRUKER_BURDE_FORSTÅTT,
                mottakersForståelse = MottakersForståelseEntity(
                    periodeRef = periodeRef,
                    mottakersForståelse = when (grad) {
                        Grad.BURDE_FORSTÅTT -> Forståelsesgrad.BURDE_FORSTÅTT
                        Grad.MÅTTE_FORSTÅ -> Forståelsesgrad.MÅTTE_FORSTÅ
                    },
                    begrunnelse = begrunnelseMottakersForståelse,
                ),
                beløpIBehold = null,
                begrunnelse = begrunnelse,
                aktsomhet = null,
                begrunnelseForUnnlatelse = kanUnnlates4XRettsgebyr.begrunnelseForUnnlatelse(),
                kanUnnlates = kanUnnlates4XRettsgebyr.tilEntity(),
                reduksjonMomenterEntity = reduksjonSærligeGrunner?.tilEntity(periodeRef),
                feilaktigEllerMangelfull = null,
                forrigePeriodeId = null,
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
                    beløpErIBehold = beløpIBehold is BeløpIBehold.DelerIBehold,
                    beløpTilbakekreves = (beløpIBehold as? BeløpIBehold.DelerIBehold)?.beløp ?: BigDecimal.ZERO,
                    begrunnelse = begrunnelseForGodTro,
                ),
                aktsomhet = null,
            )
        }

        override fun tilNyFrontendDto(): VilkaarsvurderingValgDto = GodTroDto(
            begrunnelse = begrunnelseForGodTro,
            beløpIBehold = beløpIBehold.tilFrontendDto(),
        )

        override fun oppsummerVurdering(): VurdertUtbetaling.Vilkårsvurdering {
            return VurdertUtbetaling.Vilkårsvurdering(
                aktsomhetFørUtbetaling = null,
                aktsomhetEtterUtbetaling = null,
                forårsaketAvBruker = VurdertUtbetaling.ForårsaketAvBruker.GOD_TRO,
                særligeGrunner = null,
                beløpUnnlatesUnder4Rettsgebyr = VurdertUtbetaling.JaNeiVurdering.Nei,
            )
        }

        override fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse> = beløpIBehold.påkrevdeVurderinger()

        override fun tilEntity(periodeRef: UUID): AktsomhetsvurderingEntity {
            return AktsomhetsvurderingEntity(
                vurderingType = VurderingType.IKKE_FORÅRSAKET_AV_BRUKER_GOD_TRO,
                mottakersForståelse = null,
                begrunnelse = begrunnelseForGodTro,
                beløpIBehold = beløpIBehold.tilEntity(periodeRef),
                aktsomhet = null,
                kanUnnlates = beløpIBehold.kanUnnlates()?.tilEntity(),
                reduksjonMomenterEntity = (beløpIBehold.kanUnnlates()?.reduksjonMomenter() as? ReduksjonMomenter.ReduksjonGodTro)?.tilEntity(periodeRef),
                begrunnelseForUnnlatelse = null,
                feilaktigEllerMangelfull = null,
                forrigePeriodeId = null,
            )
        }

        sealed interface BeløpIBehold {
            fun reduksjon(): Reduksjon

            fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse>

            fun tilEntity(periodeRef: UUID): GodTroEntity

            fun tilFrontendDto(): BelopIBeholdDto

            fun kanUnnlates(): KanUnnlates4xRettsgebyr?

            class HeleIBehold(
                val annetBegrunnelse: String?,
                val begrunnelse: String,
                private val kanUnnlates4XRettsgebyr: KanUnnlates4xRettsgebyr?,
            ) : BeløpIBehold {
                override fun reduksjon(): Reduksjon {
                    return kanUnnlates4XRettsgebyr?.reduksjon() ?: Reduksjon.FullstendigTilbakekreving()
                }

                override fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse> = setOf(VilkårsvurderingBegrunnelse.GOD_TRO_BELØP_I_BEHOLD)

                override fun tilFrontendDto(): BelopIBeholdDto {
                    val reduksjonArsakerDto = kanUnnlates4XRettsgebyr?.reduksjonMomenter()?.tilFrontendDto()
                    return HeleDto(
                        begrunnelse = begrunnelse,
                        reduksjon = reduksjonArsakerDto ?: SkalIkkeReduseresDto(
                            relevans = emptyList(),
                            annetBegrunnelse = null,
                            begrunnelse = "",
                        ),
                    )
                }

                override fun tilEntity(periodeRef: UUID): GodTroEntity {
                    return GodTroEntity(
                        periodeRef = periodeRef,
                        begrunnelse = begrunnelse,
                        beholdType = BeholdType.HELE_BELØPET,
                        beløpIBehold = null,
                    )
                }

                override fun kanUnnlates(): KanUnnlates4xRettsgebyr? = kanUnnlates4XRettsgebyr
            }

            class DelerIBehold(
                val beløp: BigDecimal,
                val annetBegrunnelse: String?,
                val begrunnelse: String?,
                private val kanUnnlates4XRettsgebyr: KanUnnlates4xRettsgebyr?,
            ) : BeløpIBehold {
                override fun reduksjon(): Reduksjon {
                    return kanUnnlates4XRettsgebyr?.reduksjon(beløp)
                        ?: Reduksjon.ManueltBeløp(beløp)
                }

                override fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse> = setOf(VilkårsvurderingBegrunnelse.GOD_TRO_BELØP_I_BEHOLD)

                override fun tilFrontendDto(): BelopIBeholdDto {
                    val reduksjonArsakerDto = kanUnnlates4XRettsgebyr?.reduksjonMomenter()?.tilFrontendDto()
                    return DelerDto(
                        beløp = beløp.toInt(),
                        begrunnelse = begrunnelse ?: "",
                        reduksjon = reduksjonArsakerDto ?: SkalIkkeReduseresDto(
                            relevans = emptyList(),
                            annetBegrunnelse = null,
                            begrunnelse = "",
                        ),
                    )
                }

                override fun tilEntity(periodeRef: UUID): GodTroEntity {
                    return GodTroEntity(
                        periodeRef = periodeRef,
                        begrunnelse = begrunnelse ?: "",
                        beholdType = BeholdType.DELER_AV_BELØPET,
                        beløpIBehold = beløp,
                    )
                }

                override fun kanUnnlates(): KanUnnlates4xRettsgebyr? = kanUnnlates4XRettsgebyr
            }

            class Nei(
                val begrunnelse: String?,
            ) : BeløpIBehold {
                override fun reduksjon(): Reduksjon {
                    return Reduksjon.IngenTilbakekreving()
                }

                override fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse> = setOf(VilkårsvurderingBegrunnelse.GOD_TRO_BELØP_IKKE_I_BEHOLD)

                override fun tilFrontendDto(): BelopIBeholdDto = IngentingDto(begrunnelse ?: "")

                override fun tilEntity(periodeRef: UUID): GodTroEntity {
                    return GodTroEntity(
                        periodeRef = periodeRef,
                        begrunnelse = begrunnelse ?: "",
                        beholdType = BeholdType.NEI,
                        beløpIBehold = null,
                    )
                }

                override fun kanUnnlates(): KanUnnlates4xRettsgebyr? = null
            }
        }
    }

    enum class Grad(val aktsomhet: AktsomhetDTO) {
        BURDE_FORSTÅTT(AktsomhetDTO.SIMPEL_UAKTSOMHET),
        MÅTTE_FORSTÅ(AktsomhetDTO.GROV_UAKTSOMHET),
    }

    enum class Type(override val navn: String) : Vurdering {
        Forstod("Forstod"),
        MåForstått("Må ha forstått"),
        BurdeForstått("Burde forstå"),
    }
}
