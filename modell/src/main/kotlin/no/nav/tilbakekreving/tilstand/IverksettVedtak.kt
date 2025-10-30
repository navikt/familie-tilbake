package no.nav.tilbakekreving.tilstand

import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.hendelse.IverksettelseHendelse
import no.nav.tilbakekreving.hendelse.Påminnelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.time.Duration

object IverksettVedtak : Tilstand {
    override val tidTilPåminnelse: Duration? = Duration.ofHours(1)
    override val tilbakekrevingTilstand: TilbakekrevingTilstand = TilbakekrevingTilstand.IVERKSETT_VEDTAK

    override fun behandlingsstatus(behandling: Behandling): Behandlingsstatus = Behandlingsstatus.IVERKSETTER_VEDTAK

    override fun entering(tilbakekreving: Tilbakekreving) {
        tilbakekreving.trengerIverksettelse()
        tilbakekreving.sendVedtakIverksatt()
    }

    override fun håndter(tilbakekreving: Tilbakekreving, påminnelse: Påminnelse) {
        tilbakekreving.trengerIverksettelse()
        tilbakekreving.sendVedtakIverksatt()
    }

    override fun håndter(
        tilbakekreving: Tilbakekreving,
        iverksettelseHendelse: IverksettelseHendelse,
    ) {
        tilbakekreving.byttTilstand(Avsluttet)
    }
}
