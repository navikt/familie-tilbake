package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingsvedtakService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.BrevmottakerAdresseValidering
import no.nav.familie.tilbake.dokumentbestilling.manuell.brevmottaker.ManuellBrevmottakerRepository
import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kontrakter.oppgave.Oppgavetype
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import no.nav.familie.tilbake.totrinn.TotrinnService
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class Fattevedtakssteg(
    private val behandlingskontrollService: BehandlingskontrollService,
    private val behandlingRepository: BehandlingRepository,
    private val totrinnService: TotrinnService,
    private val oppgaveTaskService: OppgaveTaskService,
    private val historikkService: HistorikkService,
    private val behandlingsvedtakService: BehandlingsvedtakService,
    private val manuellBrevmottakerRepository: ManuellBrevmottakerRepository,
) : IBehandlingssteg {
    private val log = TracedLogger.getLogger<Fattevedtakssteg>()

    override fun utførSteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId er på ${Behandlingssteg.FATTE_VEDTAK} steg")
        }
    }

    @Transactional
    override fun utførSteg(
        behandlingId: UUID,
        behandlingsstegDto: BehandlingsstegDto,
        logContext: SecureLog.Context,
    ) {
        val fatteVedtaksstegDto = behandlingsstegDto as BehandlingsstegFatteVedtaksstegDto

        // Steg 1: Validere behandlingens manuelle brevmottakere
        validerManuelleBrevmottakere(
            behandlingId = behandlingId,
            erAlleStegGodkjente = fatteVedtaksstegDto.totrinnsvurderinger.all { it.godkjent },
            logContext = logContext,
        )

        log.medContext(logContext) {
            info("Behandling $behandlingId er på ${Behandlingssteg.FATTE_VEDTAK} steg")
        }
        // Steg 2: Oppdater ansvarligBeslutter
        totrinnService.validerAnsvarligBeslutter(behandlingId, logContext)
        totrinnService.oppdaterAnsvarligBeslutter(behandlingId, logContext)

        // Steg 3: Lagre totrinnsvurderinger
        totrinnService.lagreTotrinnsvurderinger(behandlingId, fatteVedtaksstegDto.totrinnsvurderinger, logContext)

        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        // Steg 4: Lukk Godkjenne vedtak oppgaver
        oppgaveTaskService.ferdigstilleOppgaveTask(behandlingId = behandlingId, oppgavetype = Oppgavetype.GodkjenneVedtak.name)

        // Steg 5: Flytter behandling tilbake til Foreslå Vedtak om beslutter underkjente noen steg
        val finnesUnderkjenteSteg = fatteVedtaksstegDto.totrinnsvurderinger.any { !it.godkjent }
        if (finnesUnderkjenteSteg) {
            behandlingskontrollService.behandleStegPåNytt(behandlingId, Behandlingssteg.FORESLÅ_VEDTAK, logContext)

            historikkService.lagHistorikkinnslag(
                behandlingId,
                TilbakekrevingHistorikkinnslagstype.BEHANDLING_SENDT_TILBAKE_TIL_SAKSBEHANDLER,
                Aktør.Beslutter(behandling.ansvarligBeslutter!!),
                LocalDateTime.now(),
            )
            totrinnService.fjernAnsvarligBeslutter(behandlingId)
            oppgaveTaskService.opprettOppgaveTask(
                behandling,
                Oppgavetype.BehandleUnderkjentVedtak,
                behandling.ansvarligSaksbehandler,
                logContext = logContext,
            )
        } else {
            behandlingskontrollService.oppdaterBehandlingsstegStatus(
                behandlingId,
                Behandlingsstegsinfo(
                    Behandlingssteg.FATTE_VEDTAK,
                    Behandlingsstegstatus.UTFØRT,
                ),
                logContext,
            )
            historikkService.lagHistorikkinnslag(
                behandlingId,
                TilbakekrevingHistorikkinnslagstype.VEDTAK_FATTET,
                Aktør.Beslutter(behandling.ansvarligBeslutter!!),
                LocalDateTime.now(),
            )
            // Steg 6: Opprett behandlingsvedtak og oppdater behandlingsresultat
            behandlingsvedtakService.opprettBehandlingsvedtak(behandlingId)
        }
        behandlingskontrollService.fortsettBehandling(behandlingId, logContext)
    }

    @Transactional
    override fun utførStegAutomatisk(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId er på ${Behandlingssteg.FATTE_VEDTAK} steg og behandler automatisk..")
        }
        totrinnService.oppdaterAnsvarligBeslutter(behandlingId, logContext)
        totrinnService.lagreFastTotrinnsvurderingerForAutomatiskSaksbehandling(behandlingId)

        behandlingskontrollService.oppdaterBehandlingsstegStatus(
            behandlingId,
            Behandlingsstegsinfo(
                Behandlingssteg.FATTE_VEDTAK,
                Behandlingsstegstatus.UTFØRT,
            ),
            logContext,
        )
        historikkService.lagHistorikkinnslag(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.VEDTAK_FATTET,
            Aktør.Beslutter(behandlingRepository.findByIdOrThrow(behandlingId).ansvarligBeslutter!!),
            LocalDateTime.now(),
        )
        behandlingsvedtakService.opprettBehandlingsvedtak(behandlingId)
        behandlingskontrollService.fortsettBehandling(behandlingId, logContext)
    }

    @Transactional
    override fun gjenopptaSteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId gjenopptar på ${Behandlingssteg.FATTE_VEDTAK} steg")
        }
        behandlingskontrollService.oppdaterBehandlingsstegStatus(
            behandlingId,
            Behandlingsstegsinfo(
                Behandlingssteg.FATTE_VEDTAK,
                Behandlingsstegstatus.KLAR,
            ),
            logContext,
        )
    }

    override fun getBehandlingssteg(): Behandlingssteg = Behandlingssteg.FATTE_VEDTAK

    private fun validerManuelleBrevmottakere(
        behandlingId: UUID,
        erAlleStegGodkjente: Boolean,
        logContext: SecureLog.Context,
    ) {
        val manuelleBrevmottakere by lazy { manuellBrevmottakerRepository.findByBehandlingId(behandlingId) }
        if (erAlleStegGodkjente && !BrevmottakerAdresseValidering.erBrevmottakereGyldige(manuelleBrevmottakere)) {
            throw Feil(
                message = "Det finnes ugyldige brevmottakere, vi kan ikke beslutte vedtaket",
                frontendFeilmelding = "Adressen som er lagt til manuelt har ugyldig format, og vedtaksbrevet kan ikke sendes. Behandlingen må underkjennes, og saksbehandler må legge til manuell adresse på nytt.",
                logContext = logContext,
            )
        }
    }
}
