package no.nav.familie.tilbake.api.dto

import no.nav.tilbakekreving.kontrakter.tilbakekreving.Ytelsestype

data class HentFagsystemsbehandlingRequestDto(
    val ytelsestype: Ytelsestype,
    val eksternFagsakId: String,
    val eksternId: String,
)
