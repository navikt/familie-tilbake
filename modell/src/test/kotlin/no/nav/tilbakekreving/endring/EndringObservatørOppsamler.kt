package no.nav.tilbakekreving.endring

import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.math.BigDecimal
import java.util.UUID

class EndringObservatørOppsamler : EndringObservatør {
    private val statusoppdateringer = mutableMapOf<UUID, MutableList<Statusoppdatering>>()

    override fun behandlingsstatusOppdatert(
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
    ) {
        statusoppdateringer
            .computeIfAbsent(behandlingId) { mutableListOf() }
            .add(
                Statusoppdatering(
                    ansvarligSaksbehandler = ansvarligSaksbehandler,
                    vedtaksresultat = vedtaksresultat,
                ),
            )
    }

    fun statusoppdateringerFor(behandlingId: UUID): List<Statusoppdatering> {
        return statusoppdateringer[behandlingId] ?: emptyList()
    }

    data class Statusoppdatering(
        private val ansvarligSaksbehandler: String?,
        private val vedtaksresultat: Vedtaksresultat?,
    )
}
