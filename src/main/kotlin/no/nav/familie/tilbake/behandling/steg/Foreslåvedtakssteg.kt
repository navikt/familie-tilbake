package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class Foreslåvedtakssteg : IBehandlingssteg {

    @Transactional
    override fun utførSteg(behandlingId: UUID) {
        //TODO
    }

    override fun getBehandlingssteg(): Behandlingssteg {
        return Behandlingssteg.FORESLÅ_VEDTAK
    }
}
