package no.nav.tilbakekreving.endring

import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

class EndringObservatørOppsamler : EndringObservatør {
    private val statusoppdateringer = mutableMapOf<UUID, MutableList<Statusoppdatering>>()
    private val vedtakFattet = mutableMapOf<UUID, MutableList<FattetVedtak>>()

    override fun behandlingsstatusOppdatert(
        behandlingId: UUID,
        forrigeBehandlingId: UUID?,
        eksternFagsystemId: String,
        eksternBehandlingId: String,
        ytelse: Ytelse,
        tilstand: TilbakekrevingTilstand,
        behandlingstatus: Behandlingsstatus,
        vedtaksresultat: Vedtaksresultat?,
        venterPåBruker: Boolean,
        ansvarligEnhet: String?,
        ansvarligSaksbehandler: String,
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

    override fun vedtakFattet(
        behandlingId: UUID,
        forrigeBehandlingId: UUID?,
        behandlingOpprettet: OffsetDateTime,
        eksternFagsystemId: String,
        eksternBehandlingId: String,
        ytelse: Ytelse,
        vedtakFattetTidspunkt: OffsetDateTime,
        ansvarligEnhet: String?,
        ansvarligSaksbehandler: String,
        ansvarligBeslutter: String,
        vurderteUtbetalinger: List<VurdertUtbetaling>,
    ) {
        vedtakFattet.computeIfAbsent(behandlingId) { mutableListOf() }
            .add(
                FattetVedtak(
                    vurderteUtbetalinger = vurderteUtbetalinger,
                ),
            )
    }

    fun statusoppdateringerFor(behandlingId: UUID): List<Statusoppdatering> = statusoppdateringer[behandlingId] ?: emptyList()

    fun vedtakFattetFor(internId: UUID): List<FattetVedtak> = vedtakFattet[internId] ?: emptyList()

    data class Statusoppdatering(
        private val ansvarligSaksbehandler: String?,
        private val vedtaksresultat: Vedtaksresultat?,
    )

    data class FattetVedtak(
        val vurderteUtbetalinger: List<VurdertUtbetaling>,
    )
}
