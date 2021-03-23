package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.VILKÅRSVURDERING
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.UTFØRT
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class Vilkårsvurderingssteg(val behandlingskontrollService: BehandlingskontrollService) : IBehandlingssteg {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun utførSteg(behandlingId: UUID) {
        logger.info("Behandling $behandlingId er på $VILKÅRSVURDERING steg")
        behandlingskontrollService.oppdaterBehandlingsstegsstaus(behandlingId,
                                                                 Behandlingsstegsinfo(VILKÅRSVURDERING, UTFØRT))
        behandlingskontrollService.fortsettBehandling(behandlingId)
    }

    @Transactional
    override fun gjenopptaSteg(behandlingId: UUID) {
        logger.info("Behandling $behandlingId gjenopptar på ${VILKÅRSVURDERING} steg")
        behandlingskontrollService.oppdaterBehandlingsstegsstaus(behandlingId,
                                                                 Behandlingsstegsinfo(VILKÅRSVURDERING,
                                                                                      Behandlingsstegstatus.KLAR))
    }

    override fun getBehandlingssteg(): Behandlingssteg {
        return VILKÅRSVURDERING
    }
}
