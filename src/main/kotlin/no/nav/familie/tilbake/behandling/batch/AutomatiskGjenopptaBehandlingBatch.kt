package no.nav.familie.tilbake.behandling.batch

import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.log.SecureLog
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Properties

@Service
class AutomatiskGjenopptaBehandlingBatch(
    private val fagsakRepository: FagsakRepository,
    private val automatiskGjenopptaBehandlingService: AutomatiskGjenopptaBehandlingService,
    private val taskService: TracableTaskService,
    private val environment: Environment,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = "\${CRON_AUTOMATISK_GJENOPPTA}")
    @Transactional
    fun automatiskGjenopptaBehandling() {
        if (LeaderClient.isLeader() != true &&
            !environment.activeProfiles.any {
                it.contains("local") || it.contains("integrasjonstest")
            }
        ) {
            return
        }
        logger.info("Starter AutomatiskGjenopptaBehandlingBatch..")
        logger.info("Henter alle behandlinger som kan gjenopptas automatisk.")
        val behandlinger = automatiskGjenopptaBehandlingService.hentAlleBehandlingerKlarForGjenoppta()

        logger.info("Det finnes ${behandlinger.size} klar for automatisk gjenoppta")

        if (behandlinger.isNotEmpty()) {
            val alleFeiledeTasker =
                taskService.finnTasksMedStatus(
                    listOf(Status.FEILET, Status.PLUKKET, Status.KLAR_TIL_PLUKK),
                    Pageable.unpaged(),
                )
            behandlinger.forEach {
                val finnesTask =
                    alleFeiledeTasker.any { task ->
                        task.type == AutomatiskGjenopptaBehandlingTask.TYPE && task.payload == it.id.toString()
                    }
                if (!finnesTask) {
                    val fagsak = fagsakRepository.findByIdOrThrow(it.fagsakId)
                    val logContext = SecureLog.Context.medBehandling(fagsak.eksternFagsakId, it.id.toString())
                    taskService.save(
                        Task(
                            type = AutomatiskGjenopptaBehandlingTask.TYPE,
                            payload = it.id.toString(),
                            properties =
                                Properties().apply {
                                    setProperty(
                                        PropertyName.FAGSYSTEM,
                                        fagsak.fagsystem.name,
                                    )
                                },
                        ),
                        logContext,
                    )
                } else {
                    logger.info("Det finnes allerede en feilet AutomatiskGjenopptaBehandlingTask for behandlingId=${it.id}")
                }
            }
        }
        logger.info("Stopper AutomatiskGjenopptaBehandlingBatch..")
    }
}
