package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class StegService(val steg: List<IBehandlingssteg>) {

    fun håndterVarsel(behandlingId: UUID, behandlingssteg: Behandlingssteg) {
        val stegClass = hentStegInstans(behandlingssteg) as Varselssteg
        stegClass.utførSteg(behandlingId)
    }

    fun håndterGrunnlag(behandlingId: UUID, behandlingssteg: Behandlingssteg) {
        val stegClass = hentStegInstans(behandlingssteg) as MottattGrunnlagssteg
        stegClass.utførSteg(behandlingId)
    }

    fun håndterFakta(behandlingId: UUID, behandlingssteg: Behandlingssteg) {
        val stegClass = hentStegInstans(behandlingssteg) as Faktafeilutbetalingssteg
        stegClass.utførSteg(behandlingId)
    }

    fun <T : IBehandlingssteg> hentStegInstans(behandlingssteg: Behandlingssteg): T {
        val firstOrNull = steg.singleOrNull { it.behandlingssteg() == behandlingssteg }
                          ?: error("Finner ikke behandling steg $behandlingssteg")
        @Suppress("UNCHECKED_CAST")
        return firstOrNull as T
    }
}
