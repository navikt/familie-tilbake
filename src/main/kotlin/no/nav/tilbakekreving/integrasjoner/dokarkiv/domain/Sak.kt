package no.nav.tilbakekreving.integrasjoner.dokarkiv.domain

data class Sak(
    val arkivsaksnummer: String? = null,
    val arkivsaksystem: String? = null,
    val fagsakId: String? = null,
    val fagsaksystem: Fagsaksystem? = null,
    val sakstype: String? = null,
)
