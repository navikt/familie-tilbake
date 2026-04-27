package no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering

import no.nav.tilbakekreving.Rettsgebyr
import no.nav.tilbakekreving.api.v1.dto.SkalUnnlates
import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.breeeev.begrunnelse.VilkårsvurderingBegrunnelse
import no.nav.tilbakekreving.endring.VurdertUtbetaling
import no.nav.tilbakekreving.entities.KanUnnlates
import java.math.BigDecimal
import java.time.LocalDate

// §22-15 6. ledd
sealed interface KanUnnlates4xRettsgebyr {
    fun reduksjon(): Reduksjon

    fun oppsummering(): VurdertUtbetaling.JaNeiVurdering

    fun tilEntity(): KanUnnlates

    fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse>

    fun skalTilbakekreves(): Boolean

    fun tilFrontendDTO(): SkalUnnlates

    fun særligeGrunner(): ReduksjonSærligeGrunner? = null

    data object Unnlates : KanUnnlates4xRettsgebyr {
        override fun reduksjon(): Reduksjon = Reduksjon.IngenTilbakekreving()

        override fun oppsummering(): VurdertUtbetaling.JaNeiVurdering {
            return VurdertUtbetaling.JaNeiVurdering.Ja
        }

        override fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse> = setOf(VilkårsvurderingBegrunnelse.UNNLATES_4_RETTSGEBYR)

        override fun tilEntity(): KanUnnlates = KanUnnlates.UNNLATES

        override fun skalTilbakekreves(): Boolean = false

        override fun tilFrontendDTO(): SkalUnnlates = SkalUnnlates.UNNLATES
    }

    class SkalIkkeUnnlates(
        private val reduksjonSærligeGrunner: ReduksjonSærligeGrunner,
    ) : KanUnnlates4xRettsgebyr {
        override fun reduksjon(): Reduksjon = reduksjonSærligeGrunner.skalReduseres.reduksjon()

        override fun oppsummering(): VurdertUtbetaling.JaNeiVurdering {
            return VurdertUtbetaling.JaNeiVurdering.Nei
        }

        override fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse> {
            return setOf(VilkårsvurderingBegrunnelse.TILBAKEKREVES, VilkårsvurderingBegrunnelse.SKAL_IKKE_UNNLATES_4_RETTSGEBYR) + reduksjonSærligeGrunner.skalReduseres.påkrevdeVurderinger()
        }

        override fun tilEntity(): KanUnnlates = KanUnnlates.SKAL_IKKE_UNNLATES

        override fun skalTilbakekreves(): Boolean = true

        override fun tilFrontendDTO(): SkalUnnlates = SkalUnnlates.TILBAKEKREVES

        override fun særligeGrunner(): ReduksjonSærligeGrunner {
            return reduksjonSærligeGrunner
        }
    }

    class ErOver4xRettsgebyr(
        private val reduksjonSærligeGrunner: ReduksjonSærligeGrunner,
    ) : KanUnnlates4xRettsgebyr {
        override fun reduksjon(): Reduksjon {
            return reduksjonSærligeGrunner.skalReduseres.reduksjon()
        }

        override fun oppsummering(): VurdertUtbetaling.JaNeiVurdering {
            return VurdertUtbetaling.JaNeiVurdering.Nei
        }

        override fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse> = setOf(VilkårsvurderingBegrunnelse.TILBAKEKREVES) + reduksjonSærligeGrunner.skalReduseres.påkrevdeVurderinger()

        override fun tilEntity(): KanUnnlates = KanUnnlates.OVER_4_RETTSGEBYR

        override fun skalTilbakekreves(): Boolean = true

        override fun tilFrontendDTO(): SkalUnnlates = SkalUnnlates.OVER_4_RETTSGEBYR

        override fun særligeGrunner(): ReduksjonSærligeGrunner {
            return reduksjonSærligeGrunner
        }
    }

    companion object {
        fun kanUnnlates(datoForRettsgebyr: LocalDate, beløp: BigDecimal): Boolean {
            return beløp < Rettsgebyr.rettsgebyrForÅr(datoForRettsgebyr.year)!!.toBigDecimal() * 4.toBigDecimal()
        }
    }
}
