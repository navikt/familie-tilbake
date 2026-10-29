package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.behandling.TilbakekrevingRevurderingsårsak
import no.nav.tilbakekreving.behandling.TilbakekrevingRevurderingsårsak.EtterKlage
import no.nav.tilbakekreving.behandling.TilbakekrevingRevurderingsårsak.FeilutbetaltBelopEndret
import no.nav.tilbakekreving.behandling.TilbakekrevingRevurderingsårsak.Foreldelse
import no.nav.tilbakekreving.behandling.TilbakekrevingRevurderingsårsak.UtenKlage
import no.nav.tilbakekreving.behandling.TilbakekrevingRevurderingsårsakType
import no.nav.tilbakekreving.behandling.VedtakOmgjortAv
import java.util.UUID

class TilbakekrevingRevurderingEntity(
    val id: UUID,
    val behandlingRef: UUID,
    val revurderingsårsakType: TilbakekrevingRevurderingsårsakType,
    val instans: VedtakOmgjortAv?,
    val beskrivelse: String,
) {
    fun fraEntity(): TilbakekrevingRevurderingsårsak {
        return when (revurderingsårsakType) {
            TilbakekrevingRevurderingsårsakType.ETTER_KLAGE -> EtterKlage(
                årsakType = revurderingsårsakType,
                vedtakOmgjortAv = instans,
                beskrivelse = beskrivelse,
            )
            TilbakekrevingRevurderingsårsakType.FEILUTBETALT_BELØP_ENDRET -> FeilutbetaltBelopEndret(
                årsakType = revurderingsårsakType,
                vedtakOmgjortAv = instans,
                beskrivelse = beskrivelse,
            )
            TilbakekrevingRevurderingsårsakType.FORELDELSE -> Foreldelse(
                årsakType = revurderingsårsakType,
                vedtakOmgjortAv = instans,
                beskrivelse = beskrivelse,
            )
            TilbakekrevingRevurderingsårsakType.UTEN_KLAGE -> UtenKlage(
                årsakType = revurderingsårsakType,
                vedtakOmgjortAv = instans,
                beskrivelse = beskrivelse,
            )
        }
    }
}
