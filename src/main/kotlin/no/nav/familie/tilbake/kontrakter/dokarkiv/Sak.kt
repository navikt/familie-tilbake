package no.nav.familie.tilbake.kontrakter.dokarkiv

import no.nav.familie.tilbake.kontrakter.Fagsystem

data class Sak(
    val arkivsaksnummer: String? = null,
    val arkivsaksystem: String? = null,
    val fagsakId: String? = null,
    val fagsaksystem: Fagsystem? = null,
    val sakstype: String? = null,
)
