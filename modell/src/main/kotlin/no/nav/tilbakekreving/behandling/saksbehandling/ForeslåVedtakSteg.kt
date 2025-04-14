package no.nav.tilbakekreving.behandling.saksbehandling

import no.nav.tilbakekreving.api.v1.dto.BeregningsresultatDto
import no.nav.tilbakekreving.api.v1.dto.VurderingAvBrukersUttalelseDto
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.HarBrukerUttaltSeg

class ForeslåVedtakSteg : Saksbehandlingsteg<BeregningsresultatDto> {
    override val type = Behandlingssteg.FORESLÅ_VEDTAK

    override fun erFullstending(): Boolean = false

    override fun tilFrontendDto(): BeregningsresultatDto {
        return BeregningsresultatDto(
            emptyList(),
            Vedtaksresultat.INGEN_TILBAKEBETALING,
            VurderingAvBrukersUttalelseDto(
                HarBrukerUttaltSeg.NEI,
                "",
            ),
        )
    }
}
