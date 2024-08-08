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
import java.util.*

@Service
@TaskStepBeskrivelse(
    taskStepType = FerdigstillEksisterendeOppgaverOgOpprettNyTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Ferdigstill eksisterende oppgaver og opprett ny",
    triggerTidVedFeilISekunder = 300L,
)
class FerdigstillEksisterendeOppgaverOgOpprettNyTask(
    val behandlingRepository: BehandlingRepository,
    val fagsakRepository: FagsakRepository,
    val oppgaveService: OppgaveService,
    val oppgavePrioritetService: OppgavePrioritetService,
): AsyncTaskStep {

    override fun doTask(task: Task) {
        val ferdigstillEksisterendeOppgaverOgOpprettNyDto = objectMapper.readValue(task.payload, FerdigstillEksisterendeOppgaverOgOpprettNyDto::class.java)
        val behandling = behandlingRepository.findByIdOrThrow(ferdigstillEksisterendeOppgaverOgOpprettNyDto.behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val (_, finnOppgaverResponse) = oppgaveService.finnOppgave(behandling = behandling, oppgavetype = null, fagsak = fagsak)

        val oppgaverUtenomBehandleSak = finnOppgaverResponse.oppgaver.filter { it.oppgavetype !== Oppgavetype.BehandleSak.name }
        val behandleSakOppgave = finnOppgaverResponse.oppgaver.singleOrNull { it.oppgavetype == Oppgavetype.BehandleSak.name }

        oppgaverUtenomBehandleSak.forEach { oppgave ->
            oppgaveService.ferdigstillOppgave(
                behandlingId = behandling.id,
                oppgavetype = oppgave.oppgavetype?.let { Oppgavetype.valueOf(it) },
            )
        }
        if (behandleSakOppgave == null) {
            val prioritet = oppgavePrioritetService.utledOppgaveprioritet(behandling.id)
            oppgaveService.opprettOppgave(
                behandlingId = behandling.id,
                oppgavetype = Oppgavetype.BehandleSak,
                enhet = behandling.behandlendeEnhet,
                beskrivelse = ferdigstillEksisterendeOppgaverOgOpprettNyDto.beskrivelse,
                fristForFerdigstillelse = ferdigstillEksisterendeOppgaverOgOpprettNyDto.frist ?: LocalDate.now(),
                saksbehandler = null,
                prioritet = prioritet
            )
        }
    }

    companion object {
        const val TYPE = "ferdigstillEksisterendeOppgaverOgOpprettNyTask"
    }

    data class FerdigstillEksisterendeOppgaverOgOpprettNyDto(val behandlingId: UUID, val oppgavetype: Oppgavetype, val beskrivelse: String, val frist: LocalDate?)
}
