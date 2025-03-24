package no.nav.tilbakekreving.behov

import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype

data class FagsysteminfoBehov(
    val eksternFagsakId: String,
    val fagsystem: Fagsystem,
    val ytelsestype: Ytelsestype,
) : Behov
