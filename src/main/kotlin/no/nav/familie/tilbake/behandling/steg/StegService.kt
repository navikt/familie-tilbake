package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class StegService(val steg: List<IBehandlingssteg>,
                  val behandlingskontrollService: BehandlingskontrollService) {

    fun håndterVarsel(behandlingId: UUID, behandlingssteg: Behandlingssteg) {
        val stegClass = hentStegInstans(behandlingssteg) as Varselssteg
        stegClass.utførSteg(behandlingId)
    }

    fun håndterGrunnlag(behandlingId: UUID, behandlingssteg: Behandlingssteg) {
        val stegClass = hentStegInstans(behandlingssteg) as MottattGrunnlagssteg
        stegClass.utførSteg(behandlingId)
    }

    fun håndterFakta(behandlingId: UUID, behandlingssteg: Behandlingssteg) {
        val stegClass = hentStegInstans(behandlingssteg) as FaktaFeilutbetalingssteg
        stegClass.utførSteg(behandlingId)
    }

    fun håndterForeldelse(behandlingId: UUID, behandlingssteg: Behandlingssteg) {
        val stegClass = hentStegInstans(behandlingssteg) as Foreldelsessteg
        stegClass.utførSteg(behandlingId)
    }

    fun håndterVilkårsvurdering(behandlingId: UUID, behandlingssteg: Behandlingssteg) {
        val stegClass = hentStegInstans(behandlingssteg) as Vilkårsvurderingssteg
        stegClass.utførSteg(behandlingId)
    }

    fun håndterSteg(behandlingId: UUID) {
        val aktivtBehandlingssteg = behandlingskontrollService.finnAktivtSteg(behandlingId)
                                    ?: throw  Feil(message = "Behandling $behandlingId har ikke noe aktiv seg",
                                                   frontendFeilmelding = "Behandling $behandlingId har ikke noe aktiv seg")
        when (aktivtBehandlingssteg) {
            Behandlingssteg.VARSEL -> håndterVarsel(behandlingId, aktivtBehandlingssteg)
            Behandlingssteg.GRUNNLAG -> håndterGrunnlag(behandlingId, aktivtBehandlingssteg)
            Behandlingssteg.FAKTA -> håndterFakta(behandlingId, aktivtBehandlingssteg)
            Behandlingssteg.FORELDELSE -> håndterForeldelse(behandlingId, aktivtBehandlingssteg)
            Behandlingssteg.VILKÅRSVURDERING -> håndterVilkårsvurdering(behandlingId, aktivtBehandlingssteg)

            else -> throw Feil(message = "Det er ikke implementer ennå")
        }
    }

    private fun <T : IBehandlingssteg> hentStegInstans(behandlingssteg: Behandlingssteg): T {
        val firstOrNull = steg.singleOrNull { it.getBehandlingssteg() == behandlingssteg }
                          ?: error("Finner ikke behandlingssteg $behandlingssteg")
        @Suppress("UNCHECKED_CAST")
        return firstOrNull as T
    }
}
