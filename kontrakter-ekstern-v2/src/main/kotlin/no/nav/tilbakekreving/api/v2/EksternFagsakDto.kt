package no.nav.tilbakekreving.api.v2

import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO

data class EksternFagsakDto(
    val eksternId: String,
    val ytelsestype: YtelsestypeDTO,
    val fagsystem: FagsystemDTO,
)
