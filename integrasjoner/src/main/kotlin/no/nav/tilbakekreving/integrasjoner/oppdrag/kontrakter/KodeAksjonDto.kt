package no.nav.tilbakekreving.integrasjoner.oppdrag.kontrakter

import com.fasterxml.jackson.annotation.JsonProperty

enum class KodeAksjonDto {
    @JsonProperty("2")
    FINN_KRAVGRUNNLAG_FOR_DANNING_AV_NYTT_TILBAKEKREVINGSVEDTAK,

    @JsonProperty("3")
    FINN_KRAVGRUNNLAG_FOR_OMGJORING_AV_TILBAKEKREVINGSVEDTAK,

    @JsonProperty("4")
    HENT_KRAVGRUNNLAG_FOR_DANNING_AV_NYTT_TILBAKEKREVINGSVEDTAK,

    @JsonProperty("5")
    HENT_KRAVGRUNNLAG_FOR_OMGJORING_AV_TILBAKEKREVINGSVEDTAK,

    @JsonProperty("8")
    FATTE_VEDTAK,
}
