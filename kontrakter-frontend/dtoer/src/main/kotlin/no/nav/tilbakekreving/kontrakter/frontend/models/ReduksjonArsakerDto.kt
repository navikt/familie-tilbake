package no.nav.tilbakekreving.kontrakter.frontend.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonIgnoreProperties(
    value = ["erDetReduksjonÅrsaker"],
    allowSetters = true,
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "erDetReduksjonÅrsaker", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = JaSaerligeGrunnerDto::class, name = "ja"),
    JsonSubTypes.Type(value = NeiSaerligeGrunnerDto::class, name = "nei"),
    JsonSubTypes.Type(value = SkalReduseresDto::class, name = "jaGodTro"),
    JsonSubTypes.Type(value = SkalIkkeReduseresDto::class, name = "neiGodTro"),
)
sealed interface ReduksjonArsakerDto
