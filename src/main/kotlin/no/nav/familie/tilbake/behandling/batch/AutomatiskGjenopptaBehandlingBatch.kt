package no.nav.familie.tilbake.behandling.batch

import no.nav.familie.leader.LeaderClient
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AutomatiskGjenopptaBehandlingBatch(
    private val automatiskGjenopptaBehandlingService: AutomatiskGjenopptaBehandlingService,
    private val environment: Environment
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = "\${CRON_AUTOMATISK_GJENOPPTA}")
    @Transactional
    fun automatiskGjenopptaBehandling() {
        if (LeaderClient.isLeader() != true && !environment.activeProfiles.any {
            it.contains("local") || it.contains("integrasjonstest")
        }
        ) {
            return
        }
        logger.info("Starter AutomatiskGjenopptaBehandlingBatch..")
        logger.info("Henter alle behandlinger som kan gjenoppta automatisk.")
        val behandlinger = automatiskGjenopptaBehandlingService.hentAlleBehandlingerKlarForGjenoppta()

        logger.info("Det finnes ${behandlinger.size} klar for automatisk gjenoppta")
        behandlinger.forEach { automatiskGjenopptaBehandlingService.gjenopptaBehandling(it.id) }
    }
}
