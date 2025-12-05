package no.nav.tilbakekreving.endring

import no.nav.tilbakekreving.api.v2.fagsystem.ForenkletBehandlingsstatus
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

class EndringObservatørOppsamler : EndringObservatør {
    private val statusoppdateringer = mutableMapOf<UUID, MutableList<Statusoppdatering>>()
    private val vedtakFattet = mutableMapOf<UUID, MutableList<FattetVedtak>>()
    private val behandlingEndretEvents = mutableMapOf<String, MutableList<BehandlingEndret>>()

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
                    totalFeilutbetaltPeriode = totalFeilutbetaltPeriode,
                ),
            )
    }

    override fun behandlingEndret(
        behandlingId: UUID,
        vedtakGjelderId: String,
        eksternFagsakId: String,
        ytelse: Ytelse,
        eksternBehandlingId: String?,
        sakOpprettet: LocalDateTime,
        varselSendt: LocalDateTime?,
        behandlingsstatus: ForenkletBehandlingsstatus,
        totaltFeilutbetaltBeløp: BigDecimal,
        hentSaksbehandlingURL: (String) -> String,
        fullstendigPeriode: Datoperiode,
    ) {
        behandlingEndretEvents
            .computeIfAbsent(eksternFagsakId) { mutableListOf() }
            .add(BehandlingEndret(status = behandlingsstatus))
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

    fun behandlingEndretEventsFor(fagsakId: String): List<BehandlingEndret> = behandlingEndretEvents[fagsakId] ?: emptyList()

    fun statusoppdateringerFor(behandlingId: UUID): List<Statusoppdatering> = statusoppdateringer[behandlingId] ?: emptyList()

    fun vedtakFattetFor(internId: UUID): List<FattetVedtak> = vedtakFattet[internId] ?: emptyList()

    data class Statusoppdatering(
        private val ansvarligSaksbehandler: String?,
        private val vedtaksresultat: Vedtaksresultat?,
        private val totalFeilutbetaltPeriode: Datoperiode?,
    )

    data class FattetVedtak(
        val vurderteUtbetalinger: List<VurdertUtbetaling>,
    )

    data class BehandlingEndret(val status: ForenkletBehandlingsstatus)
}
