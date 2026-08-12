package no.nav.familie.tilbake.behandling.batch

import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.leader.LeaderClient
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AutomatiskGjenopptaBehandlingBatch(
    private val fagsakRepository: FagsakRepository,
    private val automatiskGjenopptaBehandlingService: AutomatiskGjenopptaBehandlingService,
    private val environment: Environment,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private val log = TracedLogger.getLogger<AutomatiskGjenopptaBehandlingBatch>()

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
            behandlinger.forEach {
                val fagsak = fagsakRepository.findByIdOrThrow(it.fagsakId)
                val logContext = SecureLog.Context.medBehandling(fagsak.eksternFagsakId, it.id.toString())
                try {
                    automatiskGjenopptaBehandlingService.gjenopptaBehandling(it.id)
                } catch (e: Exception) {
                    log.medContext(logContext) {
                        warn("Klarte ikke å automatisk gjenoppta behandling", e)
                    }
                }
            }
        }
        logger.info("Stopper AutomatiskGjenopptaBehandlingBatch..")
    }
}
