package no.nav.familie.tilbake.behandling.batch

import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AutomatiskSaksbehandlingBatch(private val automatiskSaksbehandlingService: AutomatiskSaksbehandlingService,
                                    private val taskService: TaskService,
                                    private val environment: Environment) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = "\${CRON_AUTOMATISK_SAKSBEHANDLING}")
    @Transactional
    fun behandleAutomatisk() {
        if (LeaderClient.isLeader() != true && !environment.activeProfiles.any {
                    it.contains("local") ||
                    it.contains("integrasjonstest")
                }) {
            return
        }
        logger.info("Starter AutomatiskSaksbehandlingBatch..")

        logger.info("Henter alle behandlinger som kan behandle automatisk.")
        val behandlinger = automatiskSaksbehandlingService.hentAlleBehandlingerSomKanBehandleAutomatisk()
        logger.info("Det finnes ${behandlinger.size} behandlinger som kan behandles automatisk")

        if (behandlinger.isNotEmpty()) {
            val alleFeiledeTasker = taskService.finnTasksMedStatus(listOf(Status.FEILET,
                                                                          Status.PLUKKET,
                                                                          Status.KLAR_TIL_PLUKK), Pageable.unpaged())
            behandlinger.forEach {
                val finnesTask = alleFeiledeTasker.any { task ->
                    task.type == AutomatiskSaksbehandlingTask.TYPE && task.payload == it.toString()
                }
                if (!finnesTask) {
                    taskService.save(Task(type = AutomatiskSaksbehandlingTask.TYPE,
                                          payload = it.toString()))
                } else {
                    logger.info("Det finnes allerede en feilet AutomatiskSaksbehandlingTask for samme behandlingId=$it")
                }
            }
        }
        logger.info("Stopper AutomatiskSaksbehandlingBatch..")
    }

}