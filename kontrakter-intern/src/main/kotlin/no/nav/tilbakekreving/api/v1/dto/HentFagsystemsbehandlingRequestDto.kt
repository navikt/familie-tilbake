package no.nav.tilbakekreving.api.v1.dto

import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype


data class HentFagsystemsbehandlingRequestDto(
    val ytelsestype: Ytelsestype,
    val eksternFagsakId: String,
    val eksternId: String,
)
