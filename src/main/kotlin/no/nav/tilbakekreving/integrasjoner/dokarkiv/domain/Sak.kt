package no.nav.tilbakekreving.integrasjoner.dokarkiv.domain

data class Sak(
    val arkivsaksnummer: String?,
    val arkivsaksystem: String?,
    val fagsakId: String?,
    val fagsaksystem: Fagsaksystem?,
    val sakstype: String?,
)
