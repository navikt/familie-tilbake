package no.nav.tilbakekreving.behandling

import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal
import java.util.UUID

interface BehandlingObservatør {
    fun behandlingOppdatert(
        behandlingId: UUID,
        eksternBehandlingId: String,
        vedtaksresultat: Vedtaksresultat?,
        behandlingstatus: Behandlingsstatus,
        venterPåBruker: Boolean,
        ansvarligSaksbehandler: String,
        ansvarligBeslutter: String?,
        totaltFeilutbetaltBeløp: BigDecimal?,
        totalFeilutbetaltPeriode: Datoperiode?,
    )
}
