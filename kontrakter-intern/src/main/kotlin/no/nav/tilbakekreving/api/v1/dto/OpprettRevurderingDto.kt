package no.nav.tilbakekreving.api.v1.dto

import java.util.UUID
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype

data class OpprettRevurderingDto(
    // kun brukes for tilgangskontroll
    val ytelsestype: Ytelsestype,
    val originalBehandlingId: UUID,
    val årsakstype: Behandlingsårsakstype,
)
