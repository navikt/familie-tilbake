package no.nav.familie.tilbake.service.dokumentbestilling.felles.task

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = PubliserJournalpostTask.TYPE,
                     maxAntallFeil = 3,
                     beskrivelse = "Publiserer journalpost",
                     triggerTidVedFeilISekunder = 60 * 5)
class PubliserJournalpostTask(private val integrasjonerClient: IntegrasjonerClient,
                              private val taskService: TaskService) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("${this::class.simpleName} prosesserer med id=${task.id} og metadata ${task.metadata}")

        integrasjonerClient.distribuerJournalpost(task.metadata.getProperty("journalpostId"),
                                                  Fagsystem.valueOf(task.metadata.getProperty("fagsystem")))

    }

    override fun onCompletion(task: Task) {
        taskService.save(Task(LagreBrevsporingTask.TYPE, task.payload, task.metadata))
    }


    companion object {

        const val TYPE = "publiserJournalpost"
    }
}