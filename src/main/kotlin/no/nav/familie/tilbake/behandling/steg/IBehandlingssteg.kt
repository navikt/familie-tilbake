package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.api.dto.BehandlingsstegDto
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import java.util.UUID

interface IBehandlingssteg {

    fun utførSteg(behandlingId: UUID) {
        throw Feil(message = "Noe gikk galt,er i default method implementasjon for $behandlingId")
    }

    fun utførSteg(behandlingId: UUID, behandlingsstegDto: BehandlingsstegDto) {
        throw Feil(message = "Noe gikk galt,er i default method implementasjon for $behandlingId")
    }

    fun getBehandlingssteg(): Behandlingssteg
}
