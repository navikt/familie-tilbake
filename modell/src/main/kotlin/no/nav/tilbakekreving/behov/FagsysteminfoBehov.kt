package no.nav.tilbakekreving.behov

import no.nav.tilbakekreving.fagsystem.Ytelse

data class FagsysteminfoBehov(
    val eksternFagsakId: String,
    val eksternBehandlingId: String,
    val vedtakGjelderId: String,
    val ytelse: Ytelse,
) : Behov
