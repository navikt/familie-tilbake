package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.api.dto.BehandlingsstegDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg.VILKÅRSVURDERING
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.AUTOUTFØRT
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus.UTFØRT
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.foreldelse.ForeldelseService
import no.nav.familie.tilbake.kravgrunnlag.event.EndretKravgrunnlagEvent
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class Vilkårsvurderingssteg(val behandlingskontrollService: BehandlingskontrollService,
                            val vilkårsvurderingService: VilkårsvurderingService,
                            val foreldelseService: ForeldelseService) : IBehandlingssteg {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @Transactional
    override fun utførSteg(behandlingId: UUID) {
        logger.info("Behandling $behandlingId er på $VILKÅRSVURDERING steg")
        if (harAllePerioderForeldet(behandlingId)) {
            behandlingskontrollService.oppdaterBehandlingsstegsstaus(behandlingId,
                                                                     Behandlingsstegsinfo(VILKÅRSVURDERING, AUTOUTFØRT))
            behandlingskontrollService.fortsettBehandling(behandlingId)
        }
    }

    @Transactional
    override fun utførSteg(behandlingId: UUID, behandlingsstegDto: BehandlingsstegDto) {
        logger.info("Behandling $behandlingId er på $VILKÅRSVURDERING steg")
        if (harAllePerioderForeldet(behandlingId)) {
            throw Feil(message = "Alle perioder er foreldet for $behandlingId,kan ikke behandle vilkårsvurdering",
                       frontendFeilmelding = "Alle perioder er foreldet for $behandlingId,kan ikke behandle vilkårsvurdering",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }
        vilkårsvurderingService.lagreVilkårsvurdering(behandlingId, behandlingsstegDto as BehandlingsstegVilkårsvurderingDto)

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

    @EventListener
    fun deaktiverEksisterendeVilkårsvurdering(endretKravgrunnlagEvent: EndretKravgrunnlagEvent) {
        vilkårsvurderingService.deaktiverEksisterendeVilkårsvurdering(endretKravgrunnlagEvent.behandlingId)
    }

    private fun harAllePerioderForeldet(behandlingId: UUID): Boolean {
        return foreldelseService.hentAktivVurdertForeldelse(behandlingId)
                       ?.foreldelsesperioder?.all { it.erForeldet() } ?: false
    }
}
