package no.nav.familie.tilbake.oppgave

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.FinnOppgaveRequest
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = FinnGammelBehandlingUtenOppgaveTask.TYPE,
    maxAntallFeil = 2,
    beskrivelse = "Finn gammel behandling som mangler oppgave",
    triggerTidVedFeilISekunder = 300L,
)
class FinnGammelBehandlingUtenOppgaveTask(
    private val integrasjonerClient: IntegrasjonerClient,
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
) : AsyncTaskStep {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        val dto =
            objectMapper.readValue(task.payload, FinnGammelBehandlingUtenOppgaveDto::class.java)

        val gamleBehandlinger: List<UUID> =
            behandlingRepository.finnÅpneBehandlingerOpprettetFør(
                fagsystem = dto.fagsystem,
                opprettetFørDato = LocalDateTime.now().minusMonths(2),
            ) ?: emptyList()

        log.info("Fant ${gamleBehandlinger.size} gamle åpne behandlinger. Prøver å finne ut om noen mangler oppgave.")

        gamleBehandlinger.forEach {
            val behandling = behandlingRepository.findByIdOrThrow(it)
            val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)

            val finnOppgaveRequest =
                FinnOppgaveRequest(
                    saksreferanse = behandling.eksternBrukId.toString(),
                    tema = fagsak.ytelsestype.tilTema(),
                )
            val finnOppgaveResponse = integrasjonerClient.finnOppgaver(finnOppgaveRequest)
            if (finnOppgaveResponse.antallTreffTotalt == 0L) {
                log.info("Ingen oppgave for behandlingId: ${behandling.id} fagsakId: ${fagsak.id}. Oppretter ny oppgave.")
            }
        }
    }

    data class FinnGammelBehandlingUtenOppgaveDto(
        val fagsystem: Fagsystem,
    )

    companion object {
        const val TYPE = "finnGammelBehandlingUtenOppgave"
    }
}
