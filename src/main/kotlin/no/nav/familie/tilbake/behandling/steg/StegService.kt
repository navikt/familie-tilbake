package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.api.dto.BehandlingsstegDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegFatteVedtaksstegDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class StegService(val steg: List<IBehandlingssteg>,
                  val behandlingRepository: BehandlingRepository,
                  val behandlingskontrollService: BehandlingskontrollService) {

    fun håndterSteg(behandlingId: UUID) {
        val aktivtBehandlingssteg: Behandlingssteg = hentAktivBehandlingssteg(behandlingId)

        hentStegInstans(aktivtBehandlingssteg).utførSteg(behandlingId)
    }

    fun håndterSteg(behandlingId: UUID, behandlingsstegDto: BehandlingsstegDto) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (behandling.erAvsluttet() || Behandlingsstatus.IVERKSETTER_VEDTAK == behandling.status) {
            throw Feil("Behandling med id=$behandlingId er allerede ferdig behandlet")
        }
        val behandledeSteg: Behandlingssteg = Behandlingssteg.fraNavn(behandlingsstegDto.getSteg())
        if (behandlingskontrollService.erBehandlingPåVent(behandlingId)) {
            throw Feil(message = "Behandling med id=$behandlingId er på vent, kan ikke behandle steg $behandledeSteg",
                       frontendFeilmelding = "Behandling med id=$behandlingId er på vent, kan ikke behandle steg $behandledeSteg",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }

        var aktivtBehandlingssteg: Behandlingssteg = hentAktivBehandlingssteg(behandlingId)
        // Behandling kan ikke tilbakeføres når er på FatteVedtak steg
        if (Behandlingssteg.FATTE_VEDTAK == aktivtBehandlingssteg) {
            if(behandlingsstegDto is BehandlingsstegFatteVedtaksstegDto){
                hentStegInstans(behandledeSteg).utførSteg(behandlingId, behandlingsstegDto)
            }
            return
        }
        behandlingskontrollService.behandleStegPåNytt(behandlingId, behandledeSteg)
        hentStegInstans(behandledeSteg).utførSteg(behandlingId, behandlingsstegDto)

        //sjekk om aktivtBehandlingssteg kan autoutføres
        aktivtBehandlingssteg = hentAktivBehandlingssteg(behandlingId)
        if (aktivtBehandlingssteg == Behandlingssteg.FORELDELSE || aktivtBehandlingssteg == Behandlingssteg.VILKÅRSVURDERING) {
            hentStegInstans(aktivtBehandlingssteg).utførSteg(behandlingId)
        }
    }

    fun gjenopptaSteg(behandlingId: UUID) {
        val aktivtBehandlingssteg = hentAktivBehandlingssteg(behandlingId)

        hentStegInstans(aktivtBehandlingssteg).gjenopptaSteg(behandlingId)
    }

    private fun hentAktivBehandlingssteg(behandlingId: UUID): Behandlingssteg {
        val aktivtBehandlingssteg = behandlingskontrollService.finnAktivtSteg(behandlingId)
                                    ?: throw  Feil(message = "Behandling $behandlingId har ikke noe aktiv steg",
                                                   frontendFeilmelding = "Behandling $behandlingId har ikke noe aktiv steg")
        if (aktivtBehandlingssteg !in setOf(Behandlingssteg.VARSEL,
                                            Behandlingssteg.GRUNNLAG,
                                            Behandlingssteg.FAKTA,
                                            Behandlingssteg.FORELDELSE,
                                            Behandlingssteg.VILKÅRSVURDERING,
                                            Behandlingssteg.FORESLÅ_VEDTAK,
                                            Behandlingssteg.FATTE_VEDTAK,
                                            Behandlingssteg.IVERKSETT_VEDTAK)) {
            throw Feil(message = "Steg $aktivtBehandlingssteg er ikke implementer ennå")
        }

        return aktivtBehandlingssteg
    }

    private fun hentStegInstans(behandlingssteg: Behandlingssteg): IBehandlingssteg {
        return steg.singleOrNull { it.getBehandlingssteg() == behandlingssteg }
               ?: error("Finner ikke behandlingssteg $behandlingssteg")
    }
}
