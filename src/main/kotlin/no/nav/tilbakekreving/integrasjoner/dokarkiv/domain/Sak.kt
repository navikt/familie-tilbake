package no.nav.tilbakekreving.integrasjoner.dokarkiv.domain

import no.nav.tilbakekreving.kontrakter.ytelse.DokarkivFagsaksystem

data class Sak(
    val arkivsaksnummer: String?,
    val arkivsaksystem: String?,
    val fagsakId: String?,
    val fagsaksystem: DokarkivFagsaksystem?,
    val sakstype: String?,
)
