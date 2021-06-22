package no.nav.familie.tilbake.behandling.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = OpprettManueltBehandlingTask.TYPE,
                     beskrivelse = "oppretter behandling manuelt",
                     triggerTidVedFeilISekunder = 60 * 5)
class OpprettManueltBehandlingTask : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("OpprettManueltBehandlingTask prosesserer med id=${task.id} og metadata ${task.metadata}")
    }

    companion object {

        const val TYPE = "opprettManueltBehandling"
    }

}
