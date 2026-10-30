package no.nav.tilbakekreving.kontrakter.frontend.models

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "revurderingsarsak", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = EtterKlageDto::class, name = "etter_klage"),
    JsonSubTypes.Type(value = FeilutbetaltBelopEndretDto::class, name = "feilutbetalt_belop_endret"),
    JsonSubTypes.Type(value = ForeldelseDto::class, name = "foreldelse"),
    JsonSubTypes.Type(value = UtenKlageDto::class, name = "uten_klage"),
)
sealed interface TilbakekrevingRevurderingsarsakDto
