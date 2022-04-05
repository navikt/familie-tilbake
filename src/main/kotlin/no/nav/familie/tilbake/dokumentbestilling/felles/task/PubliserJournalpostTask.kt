package no.nav.familie.tilbake.dokumentbestilling.felles.task

import no.nav.familie.http.client.RessursException
import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.kontrakter.felles.historikkinnslag.Historikkinnslagstype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.*

@Service
@TaskStepBeskrivelse(taskStepType = PubliserJournalpostTask.TYPE,
                     maxAntallFeil = 3,
                     beskrivelse = "Publiserer journalpost",
                     triggerTidVedFeilISekunder = 60 * 5L)
class PubliserJournalpostTask(private val integrasjonerClient: IntegrasjonerClient,
                              private val taskService: TaskService,
                              private val historikkTaskService: HistorikkTaskService
) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("${this::class.simpleName} prosesserer med id=${task.id} og metadata ${task.metadata}")



        try {
            integrasjonerClient.distribuerJournalpost(
                task.metadata.getProperty("journalpostId"),
                Fagsystem.valueOf(task.metadata.getProperty("fagsystem"))
            )
        } catch (ressursException: RessursException) {
            if (mottakerErIkkeDigitalOgHarUkjentAdresse(ressursException)){
                historikkTaskService.lagHistorikkTask(behandlingId = UUID.fromString(task.payload),
                    historikkinnslagstype = TilbakekrevingHistorikkinnslagstype.BREV_IKKE_SENDT_UKJENT_ADRESSE,
                    aktør = Aktør.VEDTAKSLØSNING)
            } else {
                throw ressursException
            }

        }

    }

    override fun onCompletion(task: Task) {
        taskService.save(Task(LagreBrevsporingTask.TYPE, task.payload, task.metadata))
    }

    // 400 BAD_REQUEST + kanal print er eneste måten å vite at bruker ikke er digital og har ukjent adresse fra Dokdist
    // https://nav-it.slack.com/archives/C6W9E5GPJ/p1647947002270879?thread_ts=1647936835.099329&cid=C6W9E5GPJ
    fun mottakerErIkkeDigitalOgHarUkjentAdresse(ressursException: RessursException) =
        ressursException.httpStatus == HttpStatus.BAD_REQUEST &&
                ressursException.cause?.message?.contains("Mottaker har ukjent adresse") == true

    companion object {

        const val TYPE = "publiserJournalpost"
    }
}