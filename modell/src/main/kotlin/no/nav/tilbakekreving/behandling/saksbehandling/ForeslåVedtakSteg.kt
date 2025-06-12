package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.entities.DatoperiodeEntity
import no.nav.tilbakekreving.entities.ForeslåVedtakStegEntity
import no.nav.tilbakekreving.entities.ForeslåVedtakVurderingEntity
import no.nav.tilbakekreving.entities.ForeslåVedtakVurderingEntity.ForeslåVedtakEntity
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode

class ForeslåVedtakSteg(
    private var vurdering: Vurdering,
) : Saksbehandlingsteg<Unit> {
    override val type = Behandlingssteg.FORESLÅ_VEDTAK

    override fun erFullstending(): Boolean = vurdering != Vurdering.IkkeVurdert

    internal fun håndter(vurdering: Vurdering) {
        this.vurdering = vurdering
    }

    fun tilEntity(): ForeslåVedtakStegEntity {
        return ForeslåVedtakStegEntity(
            vurdering.tilEntity(),
        )
    }

    override fun tilFrontendDto() {}

    sealed interface Vurdering {
        fun tilEntity(): ForeslåVedtakVurderingEntity

        class ForeslåVedtak(
            private val oppsummeringstekst: String?,
            private val perioderMedTekst: List<PeriodeMedTekst>,
        ) : Vurdering {
            class PeriodeMedTekst(
                val periode: Datoperiode,
                val faktaAvsnitt: String?,
                val foreldelseAvsnitt: String?,
                val vilkårAvsnitt: String?,
                val særligeGrunnerAvsnitt: String?,
                val særligeGrunnerAnnetAvsnitt: String?,
            ) {
                fun tilEntity(): ForeslåVedtakEntity.PeriodeMedTekstEntity {
                    return ForeslåVedtakEntity.PeriodeMedTekstEntity(
                        periode = DatoperiodeEntity(periode.fom.toString(), periode.tom.toString()),
                        faktaAvsnitt = faktaAvsnitt,
                        foreldelseAvsnitt = foreldelseAvsnitt,
                        vilkårAvsnitt = vilkårAvsnitt,
                        særligeGrunnerAvsnitt = særligeGrunnerAvsnitt,
                        særligeGrunnerAnnetAvsnitt = særligeGrunnerAnnetAvsnitt,
                    )
                }
            }

            override fun tilEntity(): ForeslåVedtakVurderingEntity {
                return ForeslåVedtakEntity(
                    oppsummeringstekst = oppsummeringstekst,
                    perioderMedTekst = perioderMedTekst.map { it.tilEntity() },
                )
            }
        }

        data object IkkeVurdert : Vurdering {
            override fun tilEntity(): ForeslåVedtakVurderingEntity = ForeslåVedtakVurderingEntity.IkkeVurdertEntity
        }
    }

    companion object {
        fun opprett() = ForeslåVedtakSteg(Vurdering.IkkeVurdert)
    }
}
