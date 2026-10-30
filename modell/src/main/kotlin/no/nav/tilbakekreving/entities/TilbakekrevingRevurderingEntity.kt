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
    val omgjortAv: VedtakOmgjortAv?,
    val beskrivelse: String,
) {
    fun fraEntity(): TilbakekrevingRevurderingsårsak {
        return when (revurderingsårsakType) {
            TilbakekrevingRevurderingsårsakType.ETTER_KLAGE -> EtterKlage(
                vedtakOmgjortAv = requireNotNull(omgjortAv) { "VedtakOmgjortAv må være satt for revurderingsårsak $revurderingsårsakType" },
                beskrivelse = beskrivelse,
            )
            TilbakekrevingRevurderingsårsakType.FEILUTBETALT_BELØP_ENDRET -> FeilutbetaltBelopEndret(
                vedtakOmgjortAv = requireNotNull(omgjortAv) { "VedtakOmgjortAv må være satt for revurderingsårsak $revurderingsårsakType" },
                beskrivelse = beskrivelse,
            )
            TilbakekrevingRevurderingsårsakType.FORELDELSE -> Foreldelse(
                beskrivelse = beskrivelse,
            )
            TilbakekrevingRevurderingsårsakType.UTEN_KLAGE -> UtenKlage(
                vedtakOmgjortAv = requireNotNull(omgjortAv) { "VedtakOmgjortAv må være satt for revurderingsårsak $revurderingsårsakType" },
                beskrivelse = beskrivelse,
            )
        }
    }
}
