package no.nav.tilbakekreving.api.v2.fagsystem

import java.time.LocalDateTime

interface Kafkamelding {
    val eksternFagsakId: String
    val hendelseOpprettet: LocalDateTime
}
