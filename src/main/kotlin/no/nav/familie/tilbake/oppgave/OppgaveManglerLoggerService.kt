package no.nav.familie.tilbake.oppgave

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.leader.LeaderClient
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.integration.pdl.internal.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class OppgaveManglerLoggerService(
    private val oppgaveService: OppgaveService,
    private val behandlingRepository: BehandlingRepository,
) {

    @Scheduled(initialDelay = MINUTT, fixedDelay = DØGN)
    fun loggGamleÅpneBehandlingerUtenOppgave() {
        if (LeaderClient.erLeder()) {
            val gamleBehandlinger: List<UUID> =
                behandlingRepository.finnÅpneBehandlingerIkkeEndretEtter(fagsystem = Fagsystem.EF) ?: emptyList()

            logger.info("Fant ${gamleBehandlinger.size} gamle åpne behandlinger. Prøver å finne ut om noen mangler oppgave.")
            var harIkkeOppgave = 0

            gamleBehandlinger.forEach {
                try {
                    oppgaveService.finnOppgaveForBehandlingUtenOppgaveType(it)
                } catch (e: Exception) {
                    harIkkeOppgave += 1
                }
            }
            logger.info("Ferdig med å logge gamle åpne behandlinger. ${gamleBehandlinger.size-harIkkeOppgave} har oppgave, $harIkkeOppgave har ikke oppgave.")
        }
    }
    fun LeaderClient.erLeder() = isLeader() ?: false

    companion object {
        const val DØGN = 24 * 60 * 60 * 1000L
        const val MINUTT = 60 * 1000L
    }
}