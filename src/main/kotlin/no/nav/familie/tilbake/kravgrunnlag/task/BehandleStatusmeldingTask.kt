package no.nav.familie.tilbake.kravgrunnlag.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.kravgrunnlag.KravvedtakstatusService
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandleStatusmeldingTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Håndter mottatt statusmelding fra oppdrag",
    triggerTidVedFeilISekunder = 60 * 5L,
)
class BehandleStatusmeldingTask(
    private val kravvedtakstatusService: KravvedtakstatusService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        kravvedtakstatusService.håndterMottattStatusmelding(task.payload, task.id, task.metadata)
    }

    companion object {
        const val TYPE = "behandleStatusmelding"
    }
}
