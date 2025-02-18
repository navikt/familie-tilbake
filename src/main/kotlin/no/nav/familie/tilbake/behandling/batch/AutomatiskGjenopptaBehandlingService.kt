package no.nav.familie.tilbake.behandling.batch

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
class AutomatiskGjenopptaBehandlingService(
    private val behandlingRepository: BehandlingRepository,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val historikkService: HistorikkService,
    private val stegService: StegService,
    private val oppgaveTaskService: OppgaveTaskService,
    private val logService: LogService,
) {
    private val log = TracedLogger.getLogger<AutomatiskGjenopptaBehandlingService>()

    fun hentAlleBehandlingerKlarForGjenoppta(): List<Behandling> = behandlingRepository.finnAlleBehandlingerKlarForGjenoppta(dagensdato = LocalDate.now())

    @Transactional
    fun gjenopptaBehandling(
        behandlingId: UUID,
        taskId: Long,
        taskMetadata: Properties,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val logContext = logService.contextFraBehandling(behandling.id)
        log.medContext(logContext) {
            info("AutomatiskGjenopptaBehandlingService prosesserer med id=$taskId og metadata $taskMetadata")
        }
        val behandlingsstegstilstand =
            behandlingskontrollService.finnAktivStegstilstand(behandlingId)
                ?: error("Behandling $behandlingId har ikke aktivt steg")
        val tidsfrist =
            behandlingsstegstilstand.tidsfrist
                ?: error("Behandling $behandlingId er på vent uten tidsfrist")

        historikkService.lagHistorikkinnslag(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT,
            Aktør.Vedtaksløsning,
            LocalDateTime.now(),
        )
        stegService.gjenopptaSteg(behandlingId, logContext)

        val behandlingsnystegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingId)
        if (behandlingsnystegstilstand?.behandlingssteg == Behandlingssteg.GRUNNLAG &&
            behandlingsnystegstilstand.behandlingsstegsstatus == Behandlingsstegstatus.VENTER
        ) {
            log.medContext(logContext) {
                warn(
                    "Behandling $behandlingId har ikke fått kravgrunnlag ennå " +
                        "eller mottok kravgrunnlag er sperret/avsluttet. " +
                        "Behandlingen bør analyseres og henlegges ved behov",
                )
            }
        }

        oppgaveTaskService.oppdaterOppgaveTask(
            behandlingId = behandlingId,
            beskrivelse = "Behandling er tatt av vent automatisk",
            frist = tidsfrist,
            saksbehandler = ContextService.hentSaksbehandler(logContext),
            logContext = logContext,
        )

        // oppdaterer oppgave hvis saken er fortsatt på vent,
        // f.eks saken var på vent med brukerstilbakemelding og har ikke fått kravgrunnlag
        val aktivStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingId)
        if (aktivStegstilstand?.behandlingsstegsstatus == Behandlingsstegstatus.VENTER) {
            oppgaveTaskService.oppdaterOppgaveTaskMedTriggertid(
                behandlingId = behandlingId,
                beskrivelse = aktivStegstilstand.venteårsak!!.beskrivelse,
                frist = aktivStegstilstand.tidsfrist!!,
                triggerTid = 2L,
                logContext = logContext,
            )
        }
    }
}
