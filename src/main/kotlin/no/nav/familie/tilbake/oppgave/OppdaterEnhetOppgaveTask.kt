package no.nav.familie.tilbake.oppgave

import no.nav.familie.kontrakter.felles.oppgave.Behandlingstype
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
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = OppdaterEnhetOppgaveTask.TYPE,
                     maxAntallFeil = 3,
                     beskrivelse = "Oppdaterer enhet på oppgave",
                     triggerTidVedFeilISekunder = 300L)
class OppdaterEnhetOppgaveTask(private val oppgaveService: OppgaveService,
                               private val integrasjonerClient: IntegrasjonerClient,
                               private val fagsakRepository: FagsakRepository,
                               private val behandlingRepository: BehandlingRepository) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("OppdaterEnhetOppgaveTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val enhetId = task.metadata.getProperty("enhetId")
        val beskrivelse = task.metadata.getProperty("beskrivelse")
        val behandlingId = UUID.fromString(task.payload)

        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)

        val finnOppgaveResponse =
                integrasjonerClient.finnOppgaver(FinnOppgaveRequest(behandlingstype = Behandlingstype.Tilbakekreving,
                                                                    saksreferanse = behandling.eksternBrukId.toString(),
                                                                    tema = fagsak.ytelsestype.tilTema()))
        if (finnOppgaveResponse.oppgaver.size > 1) {
            log.error("er mer enn en åpen oppgave for behandlingen")
        }
        val nyBeskrivelse = beskrivelse + "/n" + finnOppgaveResponse.oppgaver[0].beskrivelse
        oppgaveService.patchOppgave(finnOppgaveResponse.oppgaver[0].copy(tildeltEnhetsnr = enhetId,
                                                                         beskrivelse = nyBeskrivelse))
    }

    companion object {

        const val TYPE = "oppdaterEnhetOppgave"
    }
}
