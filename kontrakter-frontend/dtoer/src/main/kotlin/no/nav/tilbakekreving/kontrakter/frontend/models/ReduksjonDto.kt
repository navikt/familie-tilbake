package no.nav.tilbakekreving.kontrakter.frontend.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 *
 */
@JsonIgnoreProperties(
    value = ["reduksjon"],
    allowSetters = true,
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "reduksjon", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = SkalIkkeReduseresDto::class, name = "skalIkkeReduseres"),
    JsonSubTypes.Type(value = SkalReduseresDto::class, name = "skalReduseres"),
)
sealed interface ReduksjonDto
