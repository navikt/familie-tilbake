package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class Faktafeilutbetalingssteg : IBehandlingssteg {
    private val logger = LoggerFactory.getLogger(this::class.java)


    override fun utførSteg(behandlingId: UUID) {
        logger.info("Behandling $behandlingId er på ${Behandlingssteg.FAKTA} steg")
    }

    override fun behandlingssteg(): Behandlingssteg {
        return Behandlingssteg.FAKTA
    }
}
