package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.api.dto.BehandlingsstegDto
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class StegService(val steg: List<IBehandlingssteg>,
                  val behandlingskontrollService: BehandlingskontrollService) {

    fun håndterSteg(behandlingId: UUID,
                    behandlingsstegDto: BehandlingsstegDto? = null) {
        if (behandlingsstegDto != null) {
            val behandledeSteg = Behandlingssteg.fraKode(behandlingsstegDto.getSteg())
            hentStegInstans(behandledeSteg).utførSteg(behandlingId, behandlingsstegDto)
        }

        val aktivtBehandlingssteg = behandlingskontrollService.finnAktivtSteg(behandlingId)
                                    ?: throw  Feil(message = "Behandling $behandlingId har ikke noe aktiv steg",
                                                   frontendFeilmelding = "Behandling $behandlingId har ikke noe aktiv steg")
        if (aktivtBehandlingssteg !in setOf(Behandlingssteg.VARSEL,
                                            Behandlingssteg.GRUNNLAG,
                                            Behandlingssteg.FAKTA,
                                            Behandlingssteg.FORELDELSE,
                                            Behandlingssteg.VILKÅRSVURDERING)) {
            throw Feil(message = "Steg $aktivtBehandlingssteg er ikke implementer ennå")
        }

        //utfører steg 2 ganger for å sjekke om det nye steget kan utføres automatisk
        hentStegInstans(aktivtBehandlingssteg).utførSteg(behandlingId)
    }

    private fun hentStegInstans(behandlingssteg: Behandlingssteg): IBehandlingssteg {
        return steg.singleOrNull { it.getBehandlingssteg() == behandlingssteg }
               ?: error("Finner ikke behandlingssteg $behandlingssteg")
    }
}
