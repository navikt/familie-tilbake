package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.api.dto.BehandlingsstegDto
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class StegService(val steg: List<IBehandlingssteg>,
                  val behandlingskontrollService: BehandlingskontrollService) {

    fun håndterSteg(behandlingId: UUID) {
        val aktivtBehandlingssteg: Behandlingssteg = hentAktivBehandlingssteg(behandlingId)

        hentStegInstans(aktivtBehandlingssteg).utførSteg(behandlingId)
    }

    fun håndterSteg(behandlingId: UUID, behandlingsstegDto: BehandlingsstegDto) {
        val behandledeSteg: Behandlingssteg = Behandlingssteg.fraNavn(behandlingsstegDto.getSteg())
        if (behandlingskontrollService.erBehandlingPåVent(behandlingId)){
            throw Feil(message = "Behandling med id=$behandlingId er på vent, kan ikke behandle steg $behandledeSteg",
                       frontendFeilmelding = "Behandling med id=$behandlingId er på vent, kan ikke behandle steg $behandledeSteg",
                       httpStatus = HttpStatus.BAD_REQUEST)
        }
        behandlingskontrollService.behandleStegPåNytt(behandlingId, behandledeSteg)

        hentStegInstans(behandledeSteg).utførSteg(behandlingId, behandlingsstegDto)
        val aktivtBehandlingssteg: Behandlingssteg = hentAktivBehandlingssteg(behandlingId)
        if (aktivtBehandlingssteg == Behandlingssteg.FORELDELSE) {
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
                                            Behandlingssteg.FORESLÅ_VEDTAK)) {
            throw Feil(message = "Steg $aktivtBehandlingssteg er ikke implementer ennå")
        }

        return aktivtBehandlingssteg
    }

    private fun hentStegInstans(behandlingssteg: Behandlingssteg): IBehandlingssteg {
        return steg.singleOrNull { it.getBehandlingssteg() == behandlingssteg }
               ?: error("Finner ikke behandlingssteg $behandlingssteg")
    }
}
