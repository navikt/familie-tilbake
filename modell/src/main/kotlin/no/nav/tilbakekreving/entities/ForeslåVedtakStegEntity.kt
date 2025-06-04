package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.ForeslåVedtakSteg

data class ForeslåVedtakStegEntity(
    val vurdering: ForeslåVedtakVurderingEntity,
) {
    fun fraEntity(): ForeslåVedtakSteg {
        return ForeslåVedtakSteg(
            vurdering = vurdering.fraEntity(),
        )
    }
}

sealed interface ForeslåVedtakVurderingEntity {
    fun fraEntity(): ForeslåVedtakSteg.Vurdering

    class ForeslåVedtakEntity(
        val oppsummeringstekst: String?,
        val perioderMedTekst: List<PeriodeMedTekstEntity>,
    ) : ForeslåVedtakVurderingEntity {
        override fun fraEntity(): ForeslåVedtakSteg.Vurdering {
            return ForeslåVedtakSteg.Vurdering.ForeslåVedtak(
                oppsummeringstekst = oppsummeringstekst,
                perioderMedTekst = perioderMedTekst.map { it.fraEntity() },
            )
        }

        class PeriodeMedTekstEntity(
            val periode: DatoperiodeEntity,
            val faktaAvsnitt: String?,
            val foreldelseAvsnitt: String?,
            val vilkårAvsnitt: String?,
            val særligeGrunnerAvsnitt: String?,
            val særligeGrunnerAnnetAvsnitt: String?,
        ) {
            fun fraEntity(): ForeslåVedtakSteg.Vurdering.ForeslåVedtak.PeriodeMedTekst =
                ForeslåVedtakSteg.Vurdering.ForeslåVedtak.PeriodeMedTekst(
                    periode = periode.fraEntity(),
                    faktaAvsnitt = faktaAvsnitt,
                    foreldelseAvsnitt = foreldelseAvsnitt,
                    vilkårAvsnitt = vilkårAvsnitt,
                    særligeGrunnerAvsnitt = særligeGrunnerAvsnitt,
                    særligeGrunnerAnnetAvsnitt = særligeGrunnerAnnetAvsnitt,
                )
        }
    }

    data object IkkeVurdertEntity : ForeslåVedtakVurderingEntity {
        override fun fraEntity(): ForeslåVedtakSteg.Vurdering =
            ForeslåVedtakSteg.Vurdering.IkkeVurdert
    }
}
