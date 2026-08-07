package no.nav.tilbakekreving.assertions

import io.kotest.assertions.fail
import io.kotest.inspectors.filterMatching
import io.kotest.inspectors.forSingle
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.api.v1.dto.BehandlerRolle
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegsinfoDto
import no.nav.tilbakekreving.behandling.Behandling
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.lesContext
import no.nav.tilbakekreving.tilstand.TilBehandling

fun List<BehandlingsstegsinfoDto>.skalHaSteg(behandlingssteg: Behandlingssteg): BehandlingsstegsinfoDto {
    return this.singleOrNull { it.behandlingssteg == behandlingssteg } ?: fail("Fant ikke $behandlingssteg i ${this.map(BehandlingsstegsinfoDto::behandlingssteg)}")
}

infix fun Behandling.skalHaSteg(behandlingssteg: Behandlingssteg) = tilFrontendDto(TilBehandling, lesContext(), true, BehandlerRolle.SAKSBEHANDLER)
    .behandlingsstegsinfo filterMatching { it.behandlingssteg shouldBe behandlingssteg } forSingle {}

infix fun BehandlingsstegsinfoDto.skalHaStatus(status: Behandlingsstegstatus) = behandlingsstegstatus shouldBe status
