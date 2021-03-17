package no.nav.familie.tilbake.api.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype

interface IBehandleSteg {

    fun getSteg(): String
}

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(JsonSubTypes.Type(value = BehandlingsstegFaktaDto::class))
abstract class BehandlingsstegDto protected constructor() : IBehandleSteg

@JsonTypeName(BehandlingsstegFaktaDto.STEG_NAVN)
data class BehandlingsstegFaktaDto(val feilutbetaltePerioder: List<FaktaFeilutbetalingsperiodeDto>,
                                   val begrunnelse: String) : BehandlingsstegDto() {

    override fun getSteg(): String {
        return STEG_NAVN
    }

    companion object {

        const val STEG_NAVN = "FAKTA"
    }
}

data class FaktaFeilutbetalingsperiodeDto(val periode: Periode,
                                          val hendelsestype: Hendelsestype,
                                          val hendelsesundertype: Hendelsesundertype)

