package no.nav.tilbakekreving.kontrakter.frontend.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonIgnoreProperties(
    value = ["aktsomhet"],
    allowSetters = true,
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "aktsomhet", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = ForsettligDto::class, name = "forsettlig"),
    JsonSubTypes.Type(value = GrovtUaktsomtDto::class, name = "grovtUaktsomt"),
    JsonSubTypes.Type(value = UaktsomtDto::class, name = "uaktsomt"),
)
sealed interface AktsomhetDto
