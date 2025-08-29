package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal
import java.util.UUID

class BehandlingObservatørOppsamler : BehandlingObservatør {
    override fun behandlingOppdatert(behandlingId: UUID, eksternBehandlingId: String, vedtaksresultat: Vedtaksresultat?, behandlingstatus: Behandlingsstatus, venterPåBruker: Boolean, ansvarligSaksbehandler: String?, ansvarligBeslutter: String?, totaltFeilutbetaltBeløp: BigDecimal?, totalFeilutbetaltPeriode: Datoperiode?) {
    }
}
