package no.nav.tilbakekreving.kontrakter.frontend.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonIgnoreProperties(
    value = ["erDetSaerligeGrunner"],
    allowSetters = true,
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "erDetSaerligeGrunner", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = JaSaerligeGrunnerDto::class, name = "ja"),
    JsonSubTypes.Type(value = NeiSaerligeGrunnerDto::class, name = "nei"),
)
sealed interface SaerligeGrunnerDto
