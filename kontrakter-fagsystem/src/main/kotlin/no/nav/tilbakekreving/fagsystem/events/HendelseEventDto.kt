package no.nav.tilbakekreving.fagsystem.events

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.OffsetDateTime

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "hendelsestype",
)
@JsonSubTypes(
    JsonSubTypes.Type(value = BehandlingEndretEventDto::class, name = "behandling_endret"),
)
sealed interface HendelseEventDto {
    val versjon: Int
    val eksternFagsakId: String
    val hendelseOpprettet: OffsetDateTime
}
