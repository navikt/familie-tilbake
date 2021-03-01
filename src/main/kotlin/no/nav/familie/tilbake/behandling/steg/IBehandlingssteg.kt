package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import java.util.UUID

interface IBehandlingssteg {

    fun utførSteg(behandlingId: UUID)

    fun behandlingssteg(): Behandlingssteg
}
