package no.nav.familie.tilbake.behandling.batch

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class AutomatiskBehandlingRydderService(
    private val behandlingRepository: BehandlingRepository,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun hentGammelBehandlingerUtenKravgrunnlag(): List<Behandling> {
        logger.info("Henter gammel behandlinger uten kravgrunnlag.")
        return behandlingRepository.finnAlleGamleBehandlingerUtenKravgrunnlag()
    }
}
