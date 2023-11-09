package no.nav.familie.tilbake.api.dto

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandlingsårsakstype
import java.util.UUID

data class OpprettRevurderingDto(
    // kun brukes for tilgangskontroll
    val ytelsestype: Ytelsestype,
    val originalBehandlingId: UUID,
    val årsakstype: Behandlingsårsakstype,
)
