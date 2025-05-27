package no.nav.tilbakekreving.entities

data class ForeslåVedtakStegEntity(
    val vurdering: ForeslåVedtakVurderingEntity,
)

sealed interface ForeslåVedtakVurderingEntity {
    class ForeslåVedtakEntity(
        val oppsummeringstekst: String?,
        val perioderMedTekst: List<PeriodeMedTekstEntity>,
    ) : ForeslåVedtakVurderingEntity {
        class PeriodeMedTekstEntity(
            val periode: DatoperiodeEntity,
            val faktaAvsnitt: String?,
            val foreldelseAvsnitt: String?,
            val vilkårAvsnitt: String?,
            val særligeGrunnerAvsnitt: String?,
            val særligeGrunnerAnnetAvsnitt: String?,
        )
    }

    data object IkkeVurdertEntity : ForeslåVedtakVurderingEntity
}
