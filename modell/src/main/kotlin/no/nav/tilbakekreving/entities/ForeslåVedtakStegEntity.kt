package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.saksbehandling.ForeslåVedtakSteg

data class ForeslåVedtakStegEntity(
    val foreslåVedtakVurderingType: ForeslåVedtakVurderingType,
    val oppsummeringstekst: String?,
    val perioderMedTekst: List<PeriodeMedTekstEntity>?,
) {
    fun fraEntity(): ForeslåVedtakSteg {
        return when (foreslåVedtakVurderingType) {
            ForeslåVedtakVurderingType.FORESLÅVEDTAK -> {
                ForeslåVedtakSteg(
                    ForeslåVedtakSteg.Vurdering.ForeslåVedtak(
                        oppsummeringstekst = oppsummeringstekst,
                        perioderMedTekst = requireNotNull(perioderMedTekst) { "perioderMedTekst kreves for ForeslåVedtakSteg" }
                            .map { it.fraEntity() },
                    ),
                )
            }

            ForeslåVedtakVurderingType.IKKE_VURDERT -> {
                ForeslåVedtakSteg(
                    ForeslåVedtakSteg.Vurdering.IkkeVurdert,
                )
            }
        }
    }
}

data class PeriodeMedTekstEntity(
    val periode: DatoperiodeEntity,
    val faktaAvsnitt: String?,
    val foreldelseAvsnitt: String?,
    val vilkårAvsnitt: String?,
    val særligeGrunnerAvsnitt: String?,
    val særligeGrunnerAnnetAvsnitt: String?,
) {
    fun fraEntity() = ForeslåVedtakSteg.Vurdering.ForeslåVedtak.PeriodeMedTekst(
        periode = periode.fraEntity(),
        faktaAvsnitt = faktaAvsnitt,
        foreldelseAvsnitt = foreldelseAvsnitt,
        vilkårAvsnitt = vilkårAvsnitt,
        særligeGrunnerAvsnitt = særligeGrunnerAvsnitt,
        særligeGrunnerAnnetAvsnitt = særligeGrunnerAnnetAvsnitt,
    )
}

enum class ForeslåVedtakVurderingType {
    FORESLÅVEDTAK,
    IKKE_VURDERT,
}
