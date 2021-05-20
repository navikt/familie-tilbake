package no.nav.familie.tilbake.service.dokumentbestilling.felles.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.service.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf.BrevsporingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = LagreBrevsporingTask.TYPE,
                     maxAntallFeil = 3,
                     beskrivelse = "Lagrer brev",
                     triggerTidVedFeilISekunder = 60 * 5)
class LagreBrevsporingTask(val brevsporingService: BrevsporingService,
                           private val taskService: TaskService) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("${this::class.simpleName} prosesserer med id=${task.id} og metadata ${task.metadata}")
        val dokumentId = task.metadata.getProperty("dokumentId")
        val journalpostId = task.metadata.getProperty("journalpostId")
        val brevtype = Brevtype.valueOf(task.metadata.getProperty("brevtype"))


        brevsporingService.lagreInfoOmUtsendtBrev(UUID.fromString(task.payload),
                                                  dokumentId,
                                                  journalpostId,
                                                  brevtype)
    }

    override fun onCompletion(task: Task) {
        val mottager = Brevmottager.valueOf(task.metadata.getProperty("mottager"))
        val brevtype = Brevtype.valueOf(task.metadata.getProperty("brevtype"))
        // TODO Opprette task for Historikkinnslag.

        if (brevtype.gjelderVarsel() && mottager == Brevmottager.BRUKER) {
            taskService.save(Task(LagreVarselbrevsporingTask.TYPE, task.payload, task.metadata))
        }
    }

    companion object {

        const val TYPE = "lagreBrevsporing"
    }
}