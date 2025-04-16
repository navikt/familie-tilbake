package no.nav.tilbakekreving.behandling.saksbehandling

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

    override fun tilFrontendDto() {}

    sealed interface Vurdering {
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
            )
        }

        data object IkkeVurdert : Vurdering
    }

    companion object {
        fun opprett() = ForeslåVedtakSteg(Vurdering.IkkeVurdert)
    }
}
