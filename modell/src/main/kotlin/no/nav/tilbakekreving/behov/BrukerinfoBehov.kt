package no.nav.tilbakekreving.behov

import no.nav.tilbakekreving.fagsystem.Ytelse

data class BrukerinfoBehov(
    val ident: String,
    val ytelse: Ytelse,
) : Behov
