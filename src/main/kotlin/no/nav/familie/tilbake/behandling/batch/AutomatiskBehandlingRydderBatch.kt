package no.nav.familie.tilbake.behandling.batch

import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.behandling.task.TracableTaskService
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.leader.LeaderClient
import no.nav.familie.tilbake.log.SecureLog
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.Properties

@Service
class AutomatiskBehandlingRydderBatch(
    private val automatiskBehandlingRydderService: AutomatiskBehandlingRydderService,
    private val taskService: TracableTaskService,
    private val environment: Environment,
    private val fagsakRepository: FagsakRepository,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = "\${CRON_AUTOMATISK_GJENOPPTA}")
    fun automatiskFjerningAvGammelBehandlingerUtenKravgrunnlag() {
        if (LeaderClient.isLeader() != true &&
            !environment.activeProfiles.any {
                it.contains("local") || it.contains("integrasjonstest")
            }
        ) {
            return
        }
        logger.info("Starter AutomatiskRyddBehandlingBatch...")
        logger.info("Henter alle behandlinger som er eldre enn 8 uker og har ingen kravgrunnlag.")
        val behandlinger = automatiskBehandlingRydderService.hentGammelBehandlingerUtenKravgrunnlag()
        logger.info("Det finnes ${behandlinger.size} behandlinger eldre enn 8 uker og har ingen kravgrunnlag.")

        if (behandlinger.isNotEmpty()) {
            val alleFeiledeTasker =
                taskService.finnTasksMedStatus(
                    listOf(
                        Status.FEILET,
                        Status.KLAR_TIL_PLUKK,
                        Status.PLUKKET,
                    ),
                    Pageable.unpaged(),
                )
            behandlinger.forEach {
                val finnesTask =
                    alleFeiledeTasker.any { task ->
                        task.type == RyddBehandlingUtenKravgrunnlagTask.TYPE &&
                            task.payload == it.id.toString()
                    }
                if (!finnesTask) {
                    val fagsak = fagsakRepository.findByIdOrThrow(it.fagsakId)
                    val logContext = SecureLog.Context.medBehandling(fagsak.eksternFagsakId, it.id.toString())
                    taskService.save(
                        Task(
                            type = RyddBehandlingUtenKravgrunnlagTask.TYPE,
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
                    logger.info("Det finnes allerede en task på henleggelsen for samme behandlingId=${it.id}")
                }
            }

            logger.info("Fjernet ${behandlinger.size} behandlinger uten kravgrunnlag som var eldre enn 8 uker.")
        } else {
            logger.info("Ingen gammel behandlinger uten kravgrunnlag.")
        }
        logger.info("Rydding ferdig.")
    }
}
