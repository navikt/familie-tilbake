package no.nav.tilbakekreving.kontrakter.frontend.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonIgnoreProperties(
    value = ["tilleggsmottaker"],
    allowSetters = true,
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "tilleggsmottaker", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = FullmektigDto::class, name = "fullmektig"),
    JsonSubTypes.Type(value = VergeDto::class, name = "verge"),
)
sealed interface TilleggsmottakerDto
