package no.nav.familie.tilbake.behandling.batch

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = AutomatiskGjenopptaBehandlingTask.TYPE,
    beskrivelse = "gjenopptar behandling automatisk",
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = 60 * 5L,
)
class AutomatiskGjenopptaBehandlingTask : AsyncTaskStep {
    override fun doTask(task: Task) {}

    companion object {
        const val TYPE = "gjenoppta.behandling.automatisk"
    }
}
