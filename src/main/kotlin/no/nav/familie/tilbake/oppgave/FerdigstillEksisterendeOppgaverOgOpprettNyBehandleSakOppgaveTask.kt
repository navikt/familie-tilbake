package no.nav.familie.tilbake.oppgave

import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = FerdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgaveTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Ferdigstill eksisterende oppgaver og opprett ny",
    triggerTidVedFeilISekunder = 300L,
)
class FerdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgaveTask(
    val behandlingRepository: BehandlingRepository,
    val fagsakRepository: FagsakRepository,
    val oppgaveService: OppgaveService,
    val oppgavePrioritetService: OppgavePrioritetService,
): AsyncTaskStep {

    override fun doTask(task: Task) {
        val payload = objectMapper.readValue(task.payload, FerdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgaveDto::class.java)
        val behandling = behandlingRepository.findByIdOrThrow(payload.behandlingId)
        val godkjennVedtakOppgave = oppgaveService.hentOppgaveSomIkkeErFerdigstilt(behandling = behandling, oppgavetype = Oppgavetype.GodkjenneVedtak)
        val prioritet = oppgavePrioritetService.utledOppgaveprioritet(behandling.id)

        godkjennVedtakOppgave?.let {
            oppgaveService.ferdigstillOppgave(
                behandlingId = behandling.id,
                oppgavetype = it.oppgavetype?.let { oppgavetype -> Oppgavetype.valueOf(oppgavetype) },
            )
        }

        oppgaveService.opprettOppgave(
            behandlingId = behandling.id,
            oppgavetype = Oppgavetype.BehandleSak,
            enhet = behandling.behandlendeEnhet,
            beskrivelse = payload.beskrivelse,
            fristForFerdigstillelse = payload.frist ?: LocalDate.now(),
            saksbehandler = null,
            prioritet = prioritet
        )
    }

    companion object {
        const val TYPE = "ferdigstillEksisterendeOppgaverOgOpprettNyBehandleSakTask"
    }

    data class FerdigstillEksisterendeOppgaverOgOpprettNyBehandleSakOppgaveDto(val behandlingId: UUID, val beskrivelse: String, val frist: LocalDate?)
}
