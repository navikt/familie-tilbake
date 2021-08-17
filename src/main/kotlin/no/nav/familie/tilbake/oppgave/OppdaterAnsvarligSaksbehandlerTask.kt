package no.nav.familie.tilbake.oppgave

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
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
@TaskStepBeskrivelse(taskStepType = OppdaterAnsvarligSaksbehandlerTask.TYPE,
                     maxAntallFeil = 3,
                     beskrivelse = "Oppdaterer saksbehandler på oppgave",
                     triggerTidVedFeilISekunder = 300)
class OppdaterAnsvarligSaksbehandlerTask(private val oppgaveService: OppgaveService,
                          private val integrasjonerClient: IntegrasjonerClient,
                          private val fagsakRepository: FagsakRepository,
                          private val behandlingRepository: BehandlingRepository) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("OppdaterSaksbehandlerPåOppgaveTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val oppgavetype = Oppgavetype.valueOf(task.metadata.getProperty("oppgavetype"))
        val behandlingId = UUID.fromString(task.payload)

        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val oppgave = oppgaveService.finnOppgaveForBehandling(behandlingId, oppgavetype)

        oppgaveService.patchOppgave(oppgave.copy(tilordnetRessurs = behandling.ansvarligSaksbehandler))
    }

    companion object {

        const val TYPE = "oppdaterSaksbehandlerOppgave"
    }
}
