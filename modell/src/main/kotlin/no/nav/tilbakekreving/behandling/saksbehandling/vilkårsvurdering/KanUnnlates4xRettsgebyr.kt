package no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering

import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.breeeev.begrunnelse.VilkårsvurderingBegrunnelse
import no.nav.tilbakekreving.endring.VurdertUtbetaling
import no.nav.tilbakekreving.entities.KanUnnlates

// §22-15 6. ledd
sealed interface KanUnnlates4xRettsgebyr {
    fun reduksjon(): Reduksjon

    fun oppsummering(): VurdertUtbetaling.JaNeiVurdering

    fun tilEntity(): KanUnnlates

    fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse>

    fun skalTilbakekreves(): Boolean

    data object Unnlates : KanUnnlates4xRettsgebyr {
        override fun reduksjon(): Reduksjon = Reduksjon.IngenTilbakekreving()

        override fun oppsummering(): VurdertUtbetaling.JaNeiVurdering {
            return VurdertUtbetaling.JaNeiVurdering.Ja
        }

        override fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse> = setOf(VilkårsvurderingBegrunnelse.INGEN_TILBAKEKREVING, VilkårsvurderingBegrunnelse.UNNLATES_4_RETTSGEBYR)

        override fun tilEntity(): KanUnnlates = KanUnnlates.UNNLATES

        override fun skalTilbakekreves(): Boolean = false
    }

    class SkalIkkeUnnlates(
        val reduksjonSærligeGrunner: ReduksjonSærligeGrunner,
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
    }

    class ErOver4xRettsgebyr(
        val reduksjonSærligeGrunner: ReduksjonSærligeGrunner,
    ) : KanUnnlates4xRettsgebyr {
        override fun reduksjon(): Reduksjon {
            return reduksjonSærligeGrunner.skalReduseres.reduksjon()
        }

        override fun oppsummering(): VurdertUtbetaling.JaNeiVurdering {
            return VurdertUtbetaling.JaNeiVurdering.Nei
        }

        override fun påkrevdeVurderinger(): Set<VilkårsvurderingBegrunnelse> = setOf(VilkårsvurderingBegrunnelse.TILBAKEKREVES) + reduksjonSærligeGrunner.skalReduseres.påkrevdeVurderinger()

        override fun tilEntity(): KanUnnlates = KanUnnlates.SKAL_IKKE_UNNLATES

        override fun skalTilbakekreves(): Boolean = true
    }
}
