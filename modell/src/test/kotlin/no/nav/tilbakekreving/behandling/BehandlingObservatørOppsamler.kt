package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.behandling.saksbehandling.BehandlingsstatusModell
import no.nav.tilbakekreving.behandling.saksbehandling.Venter
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.saksbehandler.Behandler
import java.math.BigDecimal
import java.util.UUID

class BehandlingObservatørOppsamler : BehandlingObservatør {
    override fun behandlingOppdatert(behandlingId: UUID, eksternBehandlingId: String, vedtaksresultat: Vedtaksresultat?, behandlingsstatus: BehandlingsstatusModell, forrigeBehandlingsstatus: BehandlingsstatusModell?, venter: Venter?, ansvarligSaksbehandler: Behandler, ansvarligBeslutter: String?, totaltFeilutbetaltBeløp: BigDecimal, totalFeilutbetaltPeriode: Datoperiode, ansvarligEnhet: String?) {
    }
}
