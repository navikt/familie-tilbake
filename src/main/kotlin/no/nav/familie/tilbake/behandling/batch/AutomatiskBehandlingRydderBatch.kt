package no.nav.familie.tilbake.behandling.batch

import no.nav.familie.leader.LeaderClient
import no.nav.familie.tilbake.behandling.BehandlingRepository
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class AutomatiskBehandlingRydderBatch(
    private val automatiskBehandlingRydderService: AutomatiskBehandlingRydderService,
    private val behandlingRepository: BehandlingRepository,
    private val environment: Environment,
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
        logger.info("Fjerner alle behandlinger som er eldre enn 8 uker og har ingen kravgrunnlag.")
        val behandlinger = automatiskBehandlingRydderService.hentGammelBehandlingerUtenKravgrunnlag()

        if (behandlinger.isNotEmpty()) {
            behandlingRepository.deleteAll(behandlinger)
            logger.info("Fjernet ${behandlinger.size} behandlinger uten kravgrunnlag som var eldre enn 8 uker.")
        } else {
            logger.info("Ingen gammel behandlinger uten kravgrunnlag.")
        }
        logger.info("Rydding ferdig.")
    }
}
