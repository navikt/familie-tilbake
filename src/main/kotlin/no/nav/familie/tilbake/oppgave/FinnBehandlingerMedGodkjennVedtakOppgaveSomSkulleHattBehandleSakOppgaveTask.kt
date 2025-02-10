package no.nav.familie.tilbake.oppgave

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.oppgave.Oppgave
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.common.exceptionhandler.ManglerOppgaveFeil
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.SecureLog
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = FinnBehandlingerMedGodkjennVedtakOppgaveSomSkulleHattBehandleSakOppgaveTask.TYPE,
    maxAntallFeil = 2,
    beskrivelse = "Finn behandlinger med GodkjennVedtakOppgave eller ingen oppgave som skulle hatt BehandleSak oppgave",
    triggerTidVedFeilISekunder = 300L,
)
class FinnBehandlingerMedGodkjennVedtakOppgaveSomSkulleHattBehandleSakOppgaveTask(
    val behandlingRepository: BehandlingRepository,
    val oppgaveService: OppgaveService,
    val behandlingskontrollService: BehandlingskontrollService,
    private val logService: LogService,
) : AsyncTaskStep {
    val logger = LoggerFactory.getLogger(FinnBehandlingerMedGodkjennVedtakOppgaveSomSkulleHattBehandleSakOppgaveTask::class.java)

    override fun doTask(task: Task) {
        val fagsystem = Fagsystem.valueOf(task.payload)
        val behandlingerMedTilbakeførtFatteVedtakSteg = behandlingRepository.hentÅpneBehandlingerMedTilbakeførtFatteVedtakSteg(fagsystem)
        val behandlingerMedGodkjennVedtakOppgaveSomSkulleHattBehandleSakOppgave =
            behandlingerMedTilbakeførtFatteVedtakSteg
                .filter {
                    val aktivOppgave =
                        try {
                            oppgaveService.finnOppgaveForBehandlingUtenOppgaveType(it.id)
                        } catch (e: ManglerOppgaveFeil) {
                            null
                        }
                    val aktivtSteg = behandlingskontrollService.finnAktivtSteg(it.id)
                    aktivtSteg.erPåStegFørFatteVedtak() && aktivOppgave.erNullEllerGodkjenneVedtak()
                }

        behandlingerMedGodkjennVedtakOppgaveSomSkulleHattBehandleSakOppgave.forEach {
            SecureLog
                .medContext(logService.contextFraBehandling(it.id))
                .info("Behandlinger som mangler oppgave eller har feil åpen oppgave for fagsystem $fagsystem")
        }
        if (behandlingerMedGodkjennVedtakOppgaveSomSkulleHattBehandleSakOppgave.isEmpty()) {
            logger.info("Ingen behandlinger for fagsystem $fagsystem mangler oppgave eller har feil åpen oppgave.")
        }
    }

    private fun Behandlingssteg?.erPåStegFørFatteVedtak(): Boolean = this != null && this.sekvens < Behandlingssteg.FATTE_VEDTAK.sekvens

    private fun Oppgave?.erNullEllerGodkjenneVedtak(): Boolean = this == null || this.oppgavetype == Oppgavetype.GodkjenneVedtak.value

    companion object {
        const val TYPE = "finnBehandlingerMedGodkjennVedtakOppgaveSomSkulleHattBehandleSakOppgave"
    }
}
