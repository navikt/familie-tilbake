package no.nav.familie.tilbake.kontrakter.journalpost

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Sak(
    val arkivsaksnummer: String? = null,
    var arkivsaksystem: String? = null,
    val fagsakId: String? = null,
    val sakstype: String? = null,
    val fagsaksystem: String? = null,
)
