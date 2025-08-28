package no.nav.tilbakekreving.endring

import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.math.BigDecimal
import java.util.UUID

interface EndringObservatør {
    fun behandlingsstatusOppdatert(
        behandlingId: UUID,
        forrigeBehandlingId: UUID?,
        eksternFagsystemId: String,
        eksternBehandlingId: String,
        ytelse: Ytelse,
        tilstand: TilbakekrevingTilstand,
        vedtaksresultat: Vedtaksresultat?,
        venterPåBruker: Boolean,
        ansvarligEnhet: String?,
        ansvarligSaksbehandler: String?,
        ansvarligBeslutter: String?,
        totaltFeilutbetaltBeløp: BigDecimal?,
        totalFeilutbetaltPeriode: Datoperiode?,
    )
}
