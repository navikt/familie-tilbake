package no.nav.tilbakekreving.api.v2.behov

import no.nav.tilbakekreving.api.v2.Kafkamelding
import java.time.LocalDateTime

data class FagsysteminfoBehovHendelse(
    override val eksternFagsakId: String,
    val eksternBehandlingId: String,
    override val hendelseOpprettet: LocalDateTime,
) : Kafkamelding {
    override val hendelsestype: String = "fagsysteminfo_behov"
    override val versjon: Int = 1
}
