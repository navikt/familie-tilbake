package no.nav.tilbakekreving.behov

import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem

data class BrukerinfoBehov(
    val ident: String,
    val fagsystem: Fagsystem,
) : Behov
