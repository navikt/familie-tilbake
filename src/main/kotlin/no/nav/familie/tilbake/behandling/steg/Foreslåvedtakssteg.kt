package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.FeatureToggleConfig
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevService
import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kontrakter.oppgave.Oppgavetype
import no.nav.familie.tilbake.kravgrunnlag.event.EndretKravgrunnlagEvent
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.familie.tilbake.oppgave.OppgaveService
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import no.nav.familie.tilbake.totrinn.TotrinnService
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeslåVedtaksstegDto
import no.nav.tilbakekreving.kontrakter.behandling.Saksbehandlingstype
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import org.springframework.context.event.EventListener
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class Foreslåvedtakssteg(
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val vedtaksbrevService: VedtaksbrevService,
    private val oppgaveTaskService: OppgaveTaskService,
    private val historikkService: HistorikkService,
    private val oppgaveService: OppgaveService,
    private val totrinnService: TotrinnService,
    private val featureToggleService: FeatureToggleService,
) : IBehandlingssteg {
    private val log = TracedLogger.getLogger<Foreslåvedtakssteg>()

    @Transactional
    override fun utførSteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId er på $Behandlingssteg.FORESLÅ_VEDTAK} steg")
        }
        flyttBehandlingVidere(behandlingId, logContext)
    }

    @Transactional
    override fun utførSteg(
        behandlingId: UUID,
        behandlingsstegDto: BehandlingsstegDto,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId er på $Behandlingssteg.FORESLÅ_VEDTAK} steg")
        }
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        validerAtDetFinnesOppgave(behandling, logContext)

        val foreslåvedtaksstegDto = behandlingsstegDto as BehandlingsstegForeslåVedtaksstegDto
        vedtaksbrevService.lagreFriteksterFraSaksbehandler(behandlingId, foreslåvedtaksstegDto.fritekstavsnitt, logContext)

        historikkService.lagHistorikkinnslag(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.FORESLÅ_VEDTAK_VURDERT,
            Aktør.Saksbehandler.fraBehandling(behandlingId, behandlingRepository),
            LocalDateTime.now(),
        )

        flyttBehandlingVidere(behandlingId, logContext)

        ferdigstillOppgave(behandling, logContext)
        opprettGodkjennevedtakOppgave(behandlingId, logContext)

        historikkService.lagHistorikkinnslag(
            behandlingId = behandlingId,
            historikkinnslagstype =
                TilbakekrevingHistorikkinnslagstype
                    .BEHANDLING_SENDT_TIL_BESLUTTER,
            aktør = Aktør.Saksbehandler.fraBehandling(behandlingId, behandlingRepository),
            opprettetTidspunkt = LocalDateTime.now().plusSeconds(2),
        )
    }

    @Transactional
    override fun utførStegAutomatisk(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId er på ${Behandlingssteg.FORESLÅ_VEDTAK} steg og behandler automatisk..")
        }
        historikkService.lagHistorikkinnslag(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.FORESLÅ_VEDTAK_VURDERT,
            Aktør.Vedtaksløsning,
            LocalDateTime.now(),
        )
        flyttBehandlingVidere(behandlingId, logContext)

        // lukker BehandleSak oppgave og oppretter GodkjenneVedtak oppgave
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.saksbehandlingstype != Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_UNDER_4X_RETTSGEBYR) {
            ferdigstillOppgave(behandling, logContext)
            opprettGodkjennevedtakOppgave(behandlingId, logContext)
            historikkService.lagHistorikkinnslag(
                behandlingId = behandlingId,
                historikkinnslagstype =
                    TilbakekrevingHistorikkinnslagstype
                        .BEHANDLING_SENDT_TIL_BESLUTTER,
                aktør = Aktør.Vedtaksløsning,
                opprettetTidspunkt = LocalDateTime.now().plusSeconds(2),
            )
        }
    }

    @Transactional
    override fun gjenopptaSteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId gjenopptar på ${Behandlingssteg.FORESLÅ_VEDTAK} steg")
        }
        behandlingskontrollService.oppdaterBehandlingsstegStatus(
            behandlingId,
            Behandlingsstegsinfo(
                Behandlingssteg.FORESLÅ_VEDTAK,
                Behandlingsstegstatus.KLAR,
            ),
            logContext,
        )
    }

    @EventListener
    fun deaktiverEksisterendeVilkårsvurdering(endretKravgrunnlagEvent: EndretKravgrunnlagEvent) {
        vedtaksbrevService.deaktiverEksisterendeVedtaksbrevdata(endretKravgrunnlagEvent.behandlingId)
    }

    private fun flyttBehandlingVidere(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        behandlingskontrollService.oppdaterBehandlingsstegStatus(
            behandlingId,
            Behandlingsstegsinfo(
                Behandlingssteg.FORESLÅ_VEDTAK,
                Behandlingsstegstatus.UTFØRT,
            ),
            logContext,
        )
        behandlingskontrollService.fortsettBehandling(behandlingId, logContext)
    }

    // Her vet vi ikke hvorvidt vi skal ferdigstille en BehandleSak- eller en BehandleUnderkjentVedtak-oppgave.
    // Må derfor sjekke hva slags oppgave som ligger åpen og ferdigstille denne.
    private fun ferdigstillOppgave(
        behandling: Behandling,
        logContext: SecureLog.Context,
    ) {
        val muligeOppgavetyper =
            mapOf(
                Oppgavetype.BehandleSak.value to Oppgavetype.BehandleSak,
                Oppgavetype.BehandleUnderkjentVedtak.value to Oppgavetype.BehandleUnderkjentVedtak,
            )

        val fagsak = fagsakRepository.finnFagsakForBehandlingId(behandling.id)

        val (_, finnOppgaveResponse) = oppgaveService.finnOppgave(behandling = behandling, oppgavetype = null, fagsak = fagsak)
        val oppgave = finnOppgaveResponse.oppgaver.singleOrNull { muligeOppgavetyper.containsKey(it.oppgavetype) }

        if (oppgave != null) {
            val oppgavetype = muligeOppgavetyper.getValue(oppgave.oppgavetype!!)
            oppgaveTaskService.ferdigstilleOppgaveTask(behandlingId = behandling.id, oppgavetype = oppgavetype.name)
        } else {
            log.medContext(logContext) {
                warn("Finnes ingen ${Oppgavetype.BehandleSak.name} eller ${Oppgavetype.BehandleUnderkjentVedtak.name} -oppgave å ferdigstille for behandling ${behandling.id}")
            }
        }
    }

    private fun opprettGodkjennevedtakOppgave(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.saksbehandlingstype == Saksbehandlingstype.ORDINÆR) {
            val tidligerebeslutter =
                when (featureToggleService.isEnabled(FeatureToggleConfig.TIDLIGERE_BESLUTTER)) {
                    true -> totrinnService.finnForrigeBeslutterMedNyVurderingEllerNull(behandlingId)
                    false -> null
                }
            oppgaveTaskService.opprettOppgaveTask(
                behandling = behandling,
                oppgavetype = Oppgavetype.GodkjenneVedtak,
                opprettetAv = ContextService.hentSaksbehandlerNavn(),
                saksbehandler = tidligerebeslutter,
                logContext = logContext,
            )
        }
    }

    private fun validerAtDetFinnesOppgave(
        behandling: Behandling,
        logContext: SecureLog.Context,
    ) {
        val oppgavetyper: Set<Oppgavetype> = setOf(Oppgavetype.BehandleSak, Oppgavetype.BehandleUnderkjentVedtak)

        val oppgave =
            oppgavetyper.firstNotNullOfOrNull { oppgavetype ->
                oppgaveService.hentOppgaveSomIkkeErFerdigstilt(oppgavetype, behandling)
            }

        if (oppgave == null) {
            throw Feil(
                message = "Oppgaven for behandlingen er ikke tilgjengelig. Vennligst vent og prøv igjen om litt.",
                frontendFeilmelding = "Oppgaven for behandlingen er ikke tilgjengelig. Vennligst vent og prøv igjen om litt.",
                logContext = logContext,
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
    }

    override fun getBehandlingssteg(): Behandlingssteg = Behandlingssteg.FORESLÅ_VEDTAK
}
