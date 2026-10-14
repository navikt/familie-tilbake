package no.nav.tilbakekreving.kontrakter.frontend.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonIgnoreProperties(
    value = ["harBrukerUttaltSeg"],
    allowSetters = true,
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "harBrukerUttaltSeg", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = BrukeruttalelseIkkeVurdertDto::class, name = "IKKE_VURDERT"),
    JsonSubTypes.Type(value = TidligereBrukeruttalelseDto::class, name = "HAR_TIDLIGERE_VURDERING"),
    JsonSubTypes.Type(value = BrukeruttalelseDto::class, name = "JA"),
    JsonSubTypes.Type(value = IngenUttalelseDto::class, name = "NEI"),
)
sealed interface UttalelseVurderingDto

object BrukeruttalelseIkkeVurdertDto : UttalelseVurderingDto
