package no.nav.tilbakekreving.endring

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.datavarehus.saksstatistikk.sakshendelse.Behandlingstilstand
import no.nav.familie.tilbake.integration.kafka.KafkaProducer
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.config.Toggles
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.Periode
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsresultatstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

@Service
class EndringObservatørService(
    private val kafkaProducer: KafkaProducer,
    private val applicationProperties: ApplicationProperties,
) : EndringObservatør {
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
        ansvarligSaksbehandler: String?,
        ansvarligBeslutter: String?,
        totaltFeilutbetaltBeløp: BigDecimal?,
        totalFeilutbetaltPeriode: Datoperiode?,
    ) {
        val logContext = SecureLog.Context.medBehandling(eksternFagsystemId, behandlingId.toString())
        kafkaProducer.sendSaksdata(
            behandlingId,
            Behandlingstilstand(
                funksjoneltTidspunkt = OffsetDateTime.now(),
                tekniskTidspunkt = OffsetDateTime.now(),
                saksnummer = eksternFagsystemId,
                fagsystem = ytelse.tilFagsystemDTO(),
                ytelsestype = ytelse.tilYtelseDTO(),
                behandlingUuid = behandlingId,
                referertFagsaksbehandling = eksternBehandlingId,
                behandlingstype = applicationProperties.toggles.defaultWhenDisabled(Toggles::revurdering) { Behandlingstype.TILBAKEKREVING },
                behandlingsstatus = when (tilstand) {
                    TilbakekrevingTilstand.START,
                    TilbakekrevingTilstand.AVVENTER_UTSATT_BEHANDLING_MED_VARSEL,
                    TilbakekrevingTilstand.AVVENTER_UTSATT_BEHANDLING_UTEN_VARSEL,
                    TilbakekrevingTilstand.AVVENTER_KRAVGRUNNLAG,
                    TilbakekrevingTilstand.AVVENTER_FAGSYSTEMINFO,
                    TilbakekrevingTilstand.AVVENTER_BRUKERINFO,
                    TilbakekrevingTilstand.SEND_VARSELBREV,
                    -> Behandlingsstatus.OPPRETTET
                    TilbakekrevingTilstand.IVERKSETT_VEDTAK -> Behandlingsstatus.IVERKSETTER_VEDTAK
                    TilbakekrevingTilstand.TIL_BEHANDLING -> when (behandlingstatus) {
                        Behandlingsstatus.FATTER_VEDTAK -> Behandlingsstatus.FATTER_VEDTAK
                        Behandlingsstatus.UTREDES -> Behandlingsstatus.UTREDES
                        else -> throw Feil(
                            message = "Forventet ikke behandlingstatus $behandlingstatus for behandling i $tilstand",
                            logContext = logContext,
                        )
                    }
                    TilbakekrevingTilstand.AVSLUTTET -> Behandlingsstatus.AVSLUTTET
                },
                behandlingsresultat = when (vedtaksresultat) {
                    Vedtaksresultat.FULL_TILBAKEBETALING -> Behandlingsresultatstype.FULL_TILBAKEBETALING
                    Vedtaksresultat.DELVIS_TILBAKEBETALING -> Behandlingsresultatstype.DELVIS_TILBAKEBETALING
                    Vedtaksresultat.INGEN_TILBAKEBETALING -> Behandlingsresultatstype.INGEN_TILBAKEBETALING
                    null -> Behandlingsresultatstype.IKKE_FASTSATT
                },
                behandlingErManueltOpprettet = applicationProperties.toggles.defaultWhenDisabled(Toggles::manuellOpprettelse) { false },
                venterPåBruker = venterPåBruker,
                venterPåØkonomi = tilstand == TilbakekrevingTilstand.AVVENTER_KRAVGRUNNLAG,
                ansvarligEnhet = ansvarligEnhet ?: "Ukjent",
                ansvarligSaksbehandler = ansvarligSaksbehandler ?: "Ukjent",
                ansvarligBeslutter = ansvarligBeslutter,
                forrigeBehandling = forrigeBehandlingId,
                revurderingOpprettetÅrsak = applicationProperties.toggles.defaultWhenDisabled(Toggles::revurdering) { null },
                totalFeilutbetaltBeløp = totaltFeilutbetaltBeløp,
                totalFeilutbetaltPeriode = totalFeilutbetaltPeriode?.let { Periode(it.fom, it.tom) },
            ),
            logContext = SecureLog.Context.medBehandling(eksternFagsystemId, behandlingId.toString()),
        )
    }
}
