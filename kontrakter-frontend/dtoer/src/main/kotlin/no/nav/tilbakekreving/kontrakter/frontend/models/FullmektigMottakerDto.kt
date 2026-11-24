package no.nav.tilbakekreving.kontrakter.frontend.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonIgnoreProperties(
    value = ["mottakerType"],
    allowSetters = true,
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "mottakerType", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = ManuellAdresseDto::class, name = "manuell"),
    JsonSubTypes.Type(value = OrganisasjonDto::class, name = "organisasjon"),
    JsonSubTypes.Type(value = PersonDto::class, name = "person"),
)
sealed interface FullmektigMottakerDto
