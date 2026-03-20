package no.nav.tilbakekreving.assertions

import io.kotest.assertions.fail
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegsinfoDto
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg

fun List<BehandlingsstegsinfoDto>.skalHaSteg(behandlingssteg: Behandlingssteg): BehandlingsstegsinfoDto {
    return this.singleOrNull { it.behandlingssteg == behandlingssteg } ?: fail("Fant ikke $behandlingssteg i ${this.map(BehandlingsstegsinfoDto::behandlingssteg)}")
}
