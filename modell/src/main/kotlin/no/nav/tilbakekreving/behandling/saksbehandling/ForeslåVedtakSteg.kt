package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.entities.DatoperiodeEntity
import no.nav.tilbakekreving.entities.ForeslåVedtakStegEntity
import no.nav.tilbakekreving.entities.ForeslåVedtakVurderingType
import no.nav.tilbakekreving.entities.PeriodeMedTekstEntity
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode

class ForeslåVedtakSteg(
    private var vurdering: Vurdering,
) : Saksbehandlingsteg {
    override val type = Behandlingssteg.FORESLÅ_VEDTAK

    override fun erFullstendig(): Boolean = vurdering != Vurdering.IkkeVurdert

    internal fun håndter(vurdering: Vurdering) {
        this.vurdering = vurdering
    }

    fun tilEntity(): ForeslåVedtakStegEntity {
        return vurdering.tilEntity()
    }

    sealed interface Vurdering {
        fun tilEntity(): ForeslåVedtakStegEntity

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
                fun tilEntity(): PeriodeMedTekstEntity {
                    return PeriodeMedTekstEntity(
                        periode = DatoperiodeEntity(periode.fom, periode.tom),
                        faktaAvsnitt = faktaAvsnitt,
                        foreldelseAvsnitt = foreldelseAvsnitt,
                        vilkårAvsnitt = vilkårAvsnitt,
                        særligeGrunnerAvsnitt = særligeGrunnerAvsnitt,
                        særligeGrunnerAnnetAvsnitt = særligeGrunnerAnnetAvsnitt,
                    )
                }
            }

            override fun tilEntity(): ForeslåVedtakStegEntity {
                return ForeslåVedtakStegEntity(
                    foreslåVedtakVurderingType = ForeslåVedtakVurderingType.FORESLÅVEDTAK,
                    oppsummeringstekst = oppsummeringstekst,
                    perioderMedTekst = perioderMedTekst.map { it.tilEntity() },
                )
            }
        }

        data object IkkeVurdert : Vurdering {
            override fun tilEntity(): ForeslåVedtakStegEntity =
                ForeslåVedtakStegEntity(
                    foreslåVedtakVurderingType = ForeslåVedtakVurderingType.IKKE_VURDERT,
                    oppsummeringstekst = null,
                    perioderMedTekst = null,
                )
        }
    }

    companion object {
        fun opprett() = ForeslåVedtakSteg(Vurdering.IkkeVurdert)
    }
}
