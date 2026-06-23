package no.nav.tilbakekreving.kontrakter.frontend.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonIgnoreProperties(
    value = ["belopIBehold"],
    allowSetters = true,
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "belopIBehold", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = DelerDto::class, name = "deler"),
    JsonSubTypes.Type(value = HeleDto::class, name = "hele"),
    JsonSubTypes.Type(value = IngentingDto::class, name = "ingenting"),
)
sealed interface BelopIBeholdDto
