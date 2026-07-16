package no.nav.tilbakekreving.kontrakter.frontend.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonIgnoreProperties(
    value = ["unnlatelse"],
    allowSetters = true,
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "unnlatelse", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = SkalIkkeUnnlatesDto::class, name = "skalIkkeUnnlates"),
    JsonSubTypes.Type(value = SkalUnnlatesDto::class, name = "skalUnnlates"),
    JsonSubTypes.Type(value = IkkeAktueltDto::class, name = "ikkeAktuelt"),
)
sealed interface UnnlatelseDto
