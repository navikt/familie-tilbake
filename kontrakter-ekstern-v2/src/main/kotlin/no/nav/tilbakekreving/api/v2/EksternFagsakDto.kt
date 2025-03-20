package no.nav.tilbakekreving.api.v2

import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype

data class EksternFagsakDto(
    val eksternId: String,
    val ytelsestype: Ytelsestype,
    val fagsystem: Fagsystem,
)
