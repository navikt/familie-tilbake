package no.nav.tilbakekreving.api.v2

import java.time.LocalDateTime

interface Kafkamelding {
    val eksternFagsakId: String
    val hendelsestype: String
    val hendelseOpprettet: LocalDateTime
    val versjon: Int
}
