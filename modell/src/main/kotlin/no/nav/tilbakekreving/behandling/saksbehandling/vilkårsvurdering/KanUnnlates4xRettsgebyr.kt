package no.nav.tilbakekreving.behandling.saksbehandling.vilkårsvurdering

import no.nav.tilbakekreving.beregning.Reduksjon
import no.nav.tilbakekreving.breeeev.PåkrevdBegrunnelse
import no.nav.tilbakekreving.endring.VurdertUtbetaling
import no.nav.tilbakekreving.entities.KanUnnlates

// §22-15 6. ledd
sealed interface KanUnnlates4xRettsgebyr {
    fun reduksjon(): Reduksjon

    fun oppsummering(): VurdertUtbetaling.JaNeiVurdering

    fun tilEntity(): KanUnnlates

    fun påkrevdeVurderinger(): Set<PåkrevdBegrunnelse>

    data object Unnlates : KanUnnlates4xRettsgebyr {
        override fun reduksjon(): Reduksjon = Reduksjon.IngenTilbakekreving()

        override fun oppsummering(): VurdertUtbetaling.JaNeiVurdering {
            return VurdertUtbetaling.JaNeiVurdering.Ja
        }

        override fun påkrevdeVurderinger(): Set<PåkrevdBegrunnelse> = setOf(PåkrevdBegrunnelse.UNNLATES_4_RETTSGEBYR)

        override fun tilEntity(): KanUnnlates = KanUnnlates.UNNLATES
    }

    class SkalIkkeUnnlates(
        val reduksjonSærligeGrunner: ReduksjonSærligeGrunner,
    ) : KanUnnlates4xRettsgebyr {
        override fun reduksjon(): Reduksjon = reduksjonSærligeGrunner.skalReduseres.reduksjon()

        override fun oppsummering(): VurdertUtbetaling.JaNeiVurdering {
            return VurdertUtbetaling.JaNeiVurdering.Nei
        }

        override fun påkrevdeVurderinger(): Set<PåkrevdBegrunnelse> {
            return setOf(PåkrevdBegrunnelse.SKAL_IKKE_UNNLATES_4_RETTSGEBYR) + reduksjonSærligeGrunner.skalReduseres.påkrevdeVurderinger()
        }

        override fun tilEntity(): KanUnnlates = KanUnnlates.SKAL_IKKE_UNNLATES
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

        override fun påkrevdeVurderinger(): Set<PåkrevdBegrunnelse> = reduksjonSærligeGrunner.skalReduseres.påkrevdeVurderinger()

        override fun tilEntity(): KanUnnlates = KanUnnlates.SKAL_IKKE_UNNLATES
    }
}
