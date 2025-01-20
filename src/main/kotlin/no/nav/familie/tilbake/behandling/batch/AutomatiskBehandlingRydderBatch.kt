package no.nav.familie.tilbake.behandling.batch

import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Status
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.PropertyName
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.Properties

@Service
class AutomatiskBehandlingRydderBatch(
    private val automatiskBehandlingRydderService: AutomatiskBehandlingRydderService,
    private val taskService: TaskService,
    private val environment: Environment,
    private val fagsakRepository: FagsakRepository,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = "0 15 14 ? * MON")
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
                    val fagsystem = fagsakRepository.findByIdOrThrow(it.fagsakId).fagsystem
                    taskService.save(
                        Task(
                            type = RyddBehandlingUtenKravgrunnlagTask.TYPE,
                            payload = it.id.toString(),
                            properties =
                                Properties().apply {
                                    setProperty(
                                        PropertyName.FAGSYSTEM,
                                        fagsystem.name,
                                    )
                                },
                        ),
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
