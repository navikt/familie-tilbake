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

interface EndringObservatør {
    fun behandlingsstatusOppdatert(
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
    )

    fun behandlingEndret(
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
    )

    fun vedtakFattet(
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
    )
}
