package no.nav.familie.tilbake.oppgave

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.familie.tilbake.kontrakter.oppgave.FinnOppgaveRequest
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.kontrakter.Fagsystem
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
    private val log = TracedLogger.getLogger<FinnGammelBehandlingUtenOppgaveTask>()

    override fun doTask(task: Task) {
        val dto =
            objectMapper.readValue(task.payload, FinnGammelBehandlingUtenOppgaveDto::class.java)

        val gamleBehandlinger: List<UUID> =
            behandlingRepository.finnÅpneBehandlingerOpprettetFør(
                fagsystem = dto.fagsystem,
                opprettetFørDato = LocalDateTime.now().minusMonths(2),
            ) ?: emptyList()

        log.medContext(SecureLog.Context.tom()) {
            info("Fant ${gamleBehandlinger.size} gamle åpne behandlinger. Prøver å finne ut om noen mangler oppgave.")
        }

        gamleBehandlinger.forEach {
            val behandling = behandlingRepository.findByIdOrThrow(it)
            val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
            val logContext = SecureLog.Context.medBehandling(fagsak.eksternFagsakId, behandling.id.toString())

            val finnOppgaveRequest =
                FinnOppgaveRequest(
                    saksreferanse = behandling.eksternBrukId.toString(),
                    tema = fagsak.ytelsestype.tilTema(),
                )
            val finnOppgaveResponse = integrasjonerClient.finnOppgaver(finnOppgaveRequest)
            if (finnOppgaveResponse.antallTreffTotalt == 0L) {
                log.medContext(logContext) {
                    info("Ingen oppgave for behandlingId: ${behandling.id} fagsakId: ${fagsak.id}. Oppretter ny oppgave.")
                }
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
