package no.nav.tilbakekreving.kontrakter.frontend.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonIgnoreProperties(
    value = ["vurdering"],
    allowSetters = true,
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "vurdering", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = ForstoEllerBurdeForstaattDto::class, name = "forsto_eller_burde_forstått"),
    JsonSubTypes.Type(value = ForaarsaketAvMottakerDto::class, name = "forårsaket_av_mottaker"),
    JsonSubTypes.Type(value = GodTroDto::class, name = "god_tro"),
    JsonSubTypes.Type(value = VilkaarsvurderingIkkeVurdertDto::class, name = "ikke_vurdert"),
)
sealed interface VilkaarsvurderingValgDto
