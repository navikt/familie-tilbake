package no.nav.familie.tilbake.avstemming.batch

import no.nav.familie.kontrakter.felles.Fil
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.avstemming.AvstemmingService
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = AvstemmingBatchTask.TYPE,
                     beskrivelse = "Avstemming av krav.")
class AvstemmingBatchTask(val taskService: TaskService,
                          val avstemmingService: AvstemmingService,
                          val sftpBatchService: IntegrasjonerClient,
                          environment: Environment) : AsyncTaskStep {

    val applikasjon = "familie-tilbake"
    private val logger = LoggerFactory.getLogger(AvstemmingBatchTask::class.java)

    val miljø = if (environment.activeProfiles.contains("prod")) "p" else "q"

    override fun doTask(task: Task) {
        val dato = LocalDate.parse(task.payload)
        val batchRun = TYPE + "-" + UUID.randomUUID()
        logger.info("Kjører avstemming for {} i batch {}", dato, batchRun)
        val resultat = avstemmingService.oppsummer(dato)
        if (resultat != null) {
            val forDato = dato.format(DATO_FORMATTER)
            val kjøreTidspunkt = LocalDateTime.now().format(DATO_TIDSPUNKT_FORMATTER)
            val filnavn = String.format(FILNAVN_MAL, applikasjon, miljø, forDato, kjøreTidspunkt)
            val fil = Fil(filnavn, resultat)
            sftpBatchService.sendFil(fil)
            logger.info("Filen {} er overført til avstemming sftp", filnavn)
        }
    }

    override fun onCompletion(task: Task) {
        val dato = LocalDate.parse(task.payload)
        val nesteAvstemming = Task(TYPE, dato.plusDays(1).toString())
        taskService.save(nesteAvstemming)
    }

    companion object {

        const val TYPE = "task.avstemming"
        private val DATO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val DATO_TIDSPUNKT_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
        private const val FILNAVN_MAL = "%s-%s-%s-%s.csv"
    }
}