package no.nav.tilbakekreving.kontrakter.frontend.models

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonIgnoreProperties(
    value = ["adressetype"],
    allowSetters = true,
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "adressetype", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = FregAdresseDto::class, name = "freg"),
    JsonSubTypes.Type(value = NorskAdresseDto::class, name = "norsk"),
    JsonSubTypes.Type(value = UtenlandskAdresseDto::class, name = "utenlandsk"),
)
sealed interface HovedmottakerAddresseDto

data object FregAdresseDto : HovedmottakerAddresseDto
