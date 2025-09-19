package no.nav.tilbakekreving.api.v2.fagsystem.behov

import no.nav.tilbakekreving.api.v2.fagsystem.EventMetadata
import no.nav.tilbakekreving.api.v2.fagsystem.Kafkamelding
import java.time.LocalDateTime

data class FagsysteminfoBehovHendelse(
    override val eksternFagsakId: String,
    val kravgrunnlagReferanse: String,
    override val hendelseOpprettet: LocalDateTime,
) : Kafkamelding {
    companion object {
        val METADATA = EventMetadata(
            hendelsestype = "fagsysteminfo_behov",
            versjon = 1,
        )
    }
}
