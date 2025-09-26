package no.nav.tilbakekreving.api.v2.fagsystem

data class EventMetadata<K : Kafkamelding>(
    val hendelsestype: String,
    val versjon: Int,
)
