package no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering

import no.nav.tilbakekreving.api.v1.dto.VurdertSærligGrunnDto
import no.nav.tilbakekreving.behandling.saksbehandling.RelevantMomentGodTro
import no.nav.tilbakekreving.behandling.saksbehandling.SærligGrunn
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.breeeev.begrunnelse.VilkårsvurderingBegrunnelse
import no.nav.tilbakekreving.endring.VurdertUtbetaling
import no.nav.tilbakekreving.entities.ReduksjonMomenterEntity
import no.nav.tilbakekreving.entities.SkalReduseresEntity
import no.nav.tilbakekreving.entities.SkalReduseresType
import no.nav.tilbakekreving.kontrakter.frontend.models.JaSaerligeGrunnerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.NeiSaerligeGrunnerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ReduksjonArsakerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SkalIkkeReduseresDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SkalReduseresDto
import java.math.BigDecimal
import java.util.UUID

sealed interface ReduksjonMomenter {
    val begrunnelse: String

    fun tilFrontendDto(): ReduksjonArsakerDto

    fun reduksjon(): Reduksjon

    fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse>

    fun tilEntity(periodeRef: UUID): ReduksjonMomenterEntity

    class ReduksjonGodTro(
        override val begrunnelse: String,
        val grunner: Set<RelevantMomentGodTro>,
        val skalReduseres: SkalReduseres,
    ) : ReduksjonMomenter {
        override fun tilFrontendDto(): ReduksjonArsakerDto {
            return skalReduseres.tilFrontendDtoForGodTro(grunner, begrunnelse)
        }

        override fun tilEntity(periodeRef: UUID): ReduksjonMomenterEntity {
            return ReduksjonMomenterEntity(
                periodeRef = periodeRef,
                begrunnelse = begrunnelse,
                grunner = grunner.map { it.type.name },
                skalReduseres = skalReduseres.tilEntity(),
                annetBegrunnelse = (grunner.firstOrNull { it is RelevantMomentGodTro.Annet } as? RelevantMomentGodTro.Annet)?.begrunnelse,
            )
        }

        override fun reduksjon(): Reduksjon = skalReduseres.reduksjon()

        override fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse> = skalReduseres.påkrevdeVurderingerGodTro()
    }

    // §22-15 4. ledd
    class ReduksjonSærligeGrunner(
        override val begrunnelse: String,
        val grunner: Set<SærligGrunn>,
        val skalReduseres: SkalReduseres,
    ) : ReduksjonMomenter {
        override fun tilEntity(periodeRef: UUID): ReduksjonMomenterEntity {
            return ReduksjonMomenterEntity(
                periodeRef = periodeRef,
                begrunnelse = begrunnelse,
                grunner = grunner.map { it.type.name },
                skalReduseres = skalReduseres.tilEntity(),
                annetBegrunnelse = (grunner.firstOrNull { it is SærligGrunn.Annet } as? SærligGrunn.Annet)?.begrunnelse,
            )
        }

        fun oppsummerVurdering(): VurdertUtbetaling.SærligeGrunner {
            return VurdertUtbetaling.SærligeGrunner(
                beløpReduseres = skalReduseres.lagStatistikk(),
                grunner = grunner,
            )
        }

        fun vurderteGrunner(): List<VurdertSærligGrunnDto> {
            return grunner.map {
                when (it) {
                    is SærligGrunn.Annet -> VurdertSærligGrunnDto(it.type, it.begrunnelse)
                    else -> VurdertSærligGrunnDto(it.type, null)
                }
            }
        }

        override fun tilFrontendDto(): ReduksjonArsakerDto {
            return skalReduseres.tilFrontendDtoForSærligeGrunner(grunner, begrunnelse)
        }

        override fun reduksjon(): Reduksjon = skalReduseres.reduksjon()

        override fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse> = skalReduseres.påkrevdeVurderingerSærligeGrunner()
    }

    sealed interface SkalReduseres {
        fun reduksjon(): Reduksjon

        fun tilEntity(): SkalReduseresEntity

        fun lagStatistikk(): VurdertUtbetaling.JaNeiVurdering

        fun påkrevdeVurderingerSærligeGrunner(): Set<VilkårsvurderingBegrunnelse>

        fun påkrevdeVurderingerGodTro(): Set<VilkårsvurderingBegrunnelse>

        fun tilFrontendDtoForSærligeGrunner(
            grunner: Set<SærligGrunn>,
            begrunnelse: String,
        ): ReduksjonArsakerDto

        fun tilFrontendDtoForGodTro(
            grunner: Set<RelevantMomentGodTro>,
            begrunnelse: String,
        ): ReduksjonArsakerDto

        class Ja(val prosentdel: Int) : SkalReduseres {
            override fun reduksjon(): Reduksjon {
                return Reduksjon.Prosentdel(prosentdel.toBigDecimal())
            }

            override fun påkrevdeVurderingerSærligeGrunner(): Set<VilkårsvurderingBegrunnelse> = setOf(VilkårsvurderingBegrunnelse.REDUSERT_SÆRLIGE_GRUNNER)

            override fun påkrevdeVurderingerGodTro(): Set<VilkårsvurderingBegrunnelse> = setOf(VilkårsvurderingBegrunnelse.REDUSERT_GOD_TRO)

            override fun lagStatistikk(): VurdertUtbetaling.JaNeiVurdering = VurdertUtbetaling.JaNeiVurdering.Ja

            override fun tilEntity(): SkalReduseresEntity {
                return SkalReduseresEntity(SkalReduseresType.Ja, prosentdel)
            }

            override fun tilFrontendDtoForSærligeGrunner(
                grunner: Set<SærligGrunn>,
                begrunnelse: String,
            ): ReduksjonArsakerDto = JaSaerligeGrunnerDto(
                særligeGrunnerFor = grunner.map { it.tilFrontendDto() },
                prosentReduksjon = prosentdel,
                begrunnelse = begrunnelse,
                annetBegrunnelse = (grunner.firstOrNull { it is SærligGrunn.Annet } as? SærligGrunn.Annet)?.begrunnelse,
            )

            override fun tilFrontendDtoForGodTro(grunner: Set<RelevantMomentGodTro>, begrunnelse: String): ReduksjonArsakerDto = SkalReduseresDto(
                prosentReduksjon = prosentdel,
                relevans = grunner.map { it.tilFrontendDto() },
                annetBegrunnelse = (grunner.firstOrNull { it is RelevantMomentGodTro.Annet } as? RelevantMomentGodTro.Annet)?.begrunnelse,
                begrunnelse = begrunnelse,
            )
        }

        class JaAvBeløpIBehold(val prosentdel: Int, val beløpIBehold: BigDecimal) : SkalReduseres {
            override fun reduksjon(): Reduksjon {
                return Reduksjon.ProsentdelAvBeløpIBehold(prosentdel.toBigDecimal(), beløpIBehold)
            }

            override fun påkrevdeVurderingerSærligeGrunner(): Set<VilkårsvurderingBegrunnelse> = setOf(VilkårsvurderingBegrunnelse.REDUSERT_SÆRLIGE_GRUNNER)

            override fun påkrevdeVurderingerGodTro(): Set<VilkårsvurderingBegrunnelse> = setOf(VilkårsvurderingBegrunnelse.REDUSERT_GOD_TRO)

            override fun lagStatistikk(): VurdertUtbetaling.JaNeiVurdering = VurdertUtbetaling.JaNeiVurdering.Ja

            override fun tilEntity(): SkalReduseresEntity {
                return SkalReduseresEntity(SkalReduseresType.JaAvBeløpIBehold, prosentdel)
            }

            override fun tilFrontendDtoForSærligeGrunner(
                grunner: Set<SærligGrunn>,
                begrunnelse: String,
            ): ReduksjonArsakerDto = JaSaerligeGrunnerDto(
                særligeGrunnerFor = grunner.map { it.tilFrontendDto() },
                prosentReduksjon = prosentdel,
                begrunnelse = begrunnelse,
                annetBegrunnelse = (grunner.firstOrNull { it is SærligGrunn.Annet } as? SærligGrunn.Annet)?.begrunnelse,
            )

            override fun tilFrontendDtoForGodTro(grunner: Set<RelevantMomentGodTro>, begrunnelse: String): ReduksjonArsakerDto = SkalReduseresDto(
                prosentReduksjon = prosentdel,
                relevans = grunner.map { it.tilFrontendDto() },
                annetBegrunnelse = (grunner.firstOrNull { it is RelevantMomentGodTro.Annet } as? RelevantMomentGodTro.Annet)?.begrunnelse,
                begrunnelse = begrunnelse,
            )
        }

        data object Nei : SkalReduseres {
            override fun reduksjon(): Reduksjon {
                return Reduksjon.FullstendigTilbakekreving()
            }

            override fun påkrevdeVurderingerSærligeGrunner(): Set<VilkårsvurderingBegrunnelse> = setOf(VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER)

            override fun påkrevdeVurderingerGodTro(): Set<VilkårsvurderingBegrunnelse> = setOf(VilkårsvurderingBegrunnelse.IKKE_REDUSERT_GOD_TRO)

            override fun lagStatistikk(): VurdertUtbetaling.JaNeiVurdering = VurdertUtbetaling.JaNeiVurdering.Nei

            override fun tilEntity(): SkalReduseresEntity {
                return SkalReduseresEntity(SkalReduseresType.Nei, null)
            }

            override fun tilFrontendDtoForSærligeGrunner(
                grunner: Set<SærligGrunn>,
                begrunnelse: String,
            ): ReduksjonArsakerDto = NeiSaerligeGrunnerDto(
                særligeGrunnerMot = grunner.map { it.tilFrontendDto() },
                begrunnelse = begrunnelse,
                annetBegrunnelse = (grunner.firstOrNull { it is SærligGrunn.Annet } as? SærligGrunn.Annet)?.begrunnelse,
            )

            override fun tilFrontendDtoForGodTro(grunner: Set<RelevantMomentGodTro>, begrunnelse: String): ReduksjonArsakerDto = SkalIkkeReduseresDto(
                relevans = grunner.map { it.tilFrontendDto() },
                begrunnelse = begrunnelse,
                annetBegrunnelse = (grunner.firstOrNull { it is RelevantMomentGodTro.Annet } as? RelevantMomentGodTro.Annet)?.begrunnelse,
            )
        }
    }
}
