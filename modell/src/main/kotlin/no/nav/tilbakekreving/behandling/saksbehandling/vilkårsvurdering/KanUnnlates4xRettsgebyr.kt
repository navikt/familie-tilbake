package no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering

import no.nav.tilbakekreving.Rettsgebyr
import no.nav.tilbakekreving.api.v1.dto.SkalUnnlates
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.breeeev.begrunnelse.VilkårsvurderingBegrunnelse
import no.nav.tilbakekreving.endring.VurdertUtbetaling
import no.nav.tilbakekreving.entities.KanUnnlatesEntity
import no.nav.tilbakekreving.kontrakter.frontend.models.IkkeAktueltDto
import no.nav.tilbakekreving.kontrakter.frontend.models.NeiSaerligeGrunnerDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SkalIkkeUnnlatesDto
import no.nav.tilbakekreving.kontrakter.frontend.models.SkalUnnlatesDto
import no.nav.tilbakekreving.kontrakter.frontend.models.UnnlatelseDto
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal

// §22-15 6. ledd
sealed interface KanUnnlates4xRettsgebyr {
    fun reduksjon(): Reduksjon

    fun oppsummering(): VurdertUtbetaling.JaNeiVurdering

    fun tilEntity(): KanUnnlatesEntity

    fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse>

    fun skalTilbakekreves(): Boolean

    fun tilFrontendDTO(): SkalUnnlates

    fun reduksjonMomenter(): ReduksjonMomenter? = null

    fun tilFrontendDto(): UnnlatelseDto

    fun begrunnelseForUnnlatelse(): String? = null

    class Unnlates(
        private val begrunnelseForUnnlatelse: String?,
    ) : KanUnnlates4xRettsgebyr {
        override fun reduksjon(): Reduksjon = Reduksjon.IngenTilbakekreving()

        override fun oppsummering(): VurdertUtbetaling.JaNeiVurdering {
            return VurdertUtbetaling.JaNeiVurdering.Ja
        }

        override fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse> = setOf(VilkårsvurderingBegrunnelse.UNNLATES_4_RETTSGEBYR)

        override fun tilEntity(): KanUnnlatesEntity = KanUnnlatesEntity.UNNLATES

        override fun skalTilbakekreves(): Boolean = false

        override fun tilFrontendDTO(): SkalUnnlates = SkalUnnlates.UNNLATES

        override fun tilFrontendDto(): UnnlatelseDto {
            return SkalUnnlatesDto(
                begrunnelse = begrunnelseForUnnlatelse!!,
            )
        }

        override fun begrunnelseForUnnlatelse() = begrunnelseForUnnlatelse
    }

    class SkalIkkeUnnlates(
        private val reduksjonMomenter: ReduksjonMomenter,
    ) : KanUnnlates4xRettsgebyr {
        override fun reduksjon(): Reduksjon = reduksjonMomenter.reduksjon()

        override fun oppsummering(): VurdertUtbetaling.JaNeiVurdering {
            return VurdertUtbetaling.JaNeiVurdering.Nei
        }

        override fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse> {
            return setOf(VilkårsvurderingBegrunnelse.TILBAKEKREVES, VilkårsvurderingBegrunnelse.SKAL_IKKE_UNNLATES_4_RETTSGEBYR) +
                reduksjonMomenter.påkrevdeVurderinger()
        }

        override fun tilEntity(): KanUnnlatesEntity = KanUnnlatesEntity.SKAL_IKKE_UNNLATES

        override fun skalTilbakekreves(): Boolean = true

        override fun tilFrontendDTO(): SkalUnnlates = SkalUnnlates.TILBAKEKREVES

        override fun tilFrontendDto(): UnnlatelseDto {
            return SkalIkkeUnnlatesDto(
                begrunnelse = reduksjonMomenter.begrunnelse,
                erDetSærligeGrunner = reduksjonMomenter.tilFrontendDto(),
            )
        }

        override fun reduksjonMomenter(): ReduksjonMomenter {
            return reduksjonMomenter
        }
    }

    class ErOver4xRettsgebyr(
        private val reduksjonSærligeGrunner: ReduksjonMomenter.ReduksjonSærligeGrunner,
    ) : KanUnnlates4xRettsgebyr {
        override fun reduksjon(): Reduksjon {
            return reduksjonSærligeGrunner.skalReduseres.reduksjon()
        }

        override fun oppsummering(): VurdertUtbetaling.JaNeiVurdering {
            return VurdertUtbetaling.JaNeiVurdering.Nei
        }

        override fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse> = setOf(VilkårsvurderingBegrunnelse.TILBAKEKREVES) + reduksjonSærligeGrunner.skalReduseres.påkrevdeVurderinger()

        override fun tilEntity(): KanUnnlatesEntity = KanUnnlatesEntity.OVER_4_RETTSGEBYR

        override fun skalTilbakekreves(): Boolean = true

        override fun tilFrontendDTO(): SkalUnnlates = SkalUnnlates.OVER_4_RETTSGEBYR

        override fun tilFrontendDto(): UnnlatelseDto {
            return IkkeAktueltDto(reduksjonSærligeGrunner.tilFrontendDto())
        }

        override fun reduksjonMomenter(): ReduksjonMomenter.ReduksjonSærligeGrunner {
            return reduksjonSærligeGrunner
        }
    }

    object IkkeVurdert : KanUnnlates4xRettsgebyr {
        override fun reduksjon(): Reduksjon {
            return Reduksjon.FullstendigTilbakekreving()
        }

        override fun oppsummering(): VurdertUtbetaling.JaNeiVurdering {
            return VurdertUtbetaling.JaNeiVurdering.Nei
        }

        override fun tilEntity(): KanUnnlatesEntity {
            return KanUnnlatesEntity.IKKE_VURDERT
        }

        override fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse> {
            return setOf(VilkårsvurderingBegrunnelse.TILBAKEKREVES, VilkårsvurderingBegrunnelse.SKAL_IKKE_UNNLATES_4_RETTSGEBYR, VilkårsvurderingBegrunnelse.IKKE_REDUSERT_SÆRLIGE_GRUNNER)
        }

        override fun skalTilbakekreves(): Boolean {
            return true
        }

        override fun tilFrontendDTO(): SkalUnnlates {
            return SkalUnnlates.TILBAKEKREVES
        }

        override fun tilFrontendDto(): UnnlatelseDto {
            return IkkeAktueltDto(
                NeiSaerligeGrunnerDto(
                    særligeGrunnerMot = emptyList(),
                    begrunnelse = "Vurdering av særlige grunner har tidligere ikke vært mulig",
                    annetBegrunnelse = null,
                ),
            )
        }
    }

    companion object {
        fun kanUnnlates(fullstendigVedtaksperiode: Datoperiode, årForRettsgebyr: Int?, beløp: BigDecimal) = when {
            beløp < Rettsgebyr.fireRettsgebyrForÅr(fullstendigVedtaksperiode.fom.year).toBigDecimal() -> KanUnnlates.Ja
            beløp >= Rettsgebyr.fireRettsgebyrForÅr(fullstendigVedtaksperiode.tom.year).toBigDecimal() -> KanUnnlates.Nei
            årForRettsgebyr == null -> KanUnnlates.Usikkert
            beløp < Rettsgebyr.fireRettsgebyrForÅr(årForRettsgebyr).toBigDecimal() -> KanUnnlates.Ja
            else -> KanUnnlates.Nei
        }
    }

    enum class KanUnnlates {
        Ja,
        Nei,
        Usikkert,
    }
}
