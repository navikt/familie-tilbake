package no.nav.tilbakekreving.api.v1.dto

import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO

data class HentFagsystemsbehandlingRequestDto(
    val ytelsestype: YtelsestypeDTO,
    val eksternFagsakId: String,
    val eksternId: String,
)
