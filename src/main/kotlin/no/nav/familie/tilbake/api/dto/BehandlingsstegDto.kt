package no.nav.familie.tilbake.api.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(JsonSubTypes.Type(value = BehandlingsstegFaktaDto::class),
              JsonSubTypes.Type(value = BehandlingsstegForeldelseDto::class))
abstract class BehandlingsstegDto protected constructor() {

    abstract fun getSteg(): String
}

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

data class FaktaFeilutbetalingsperiodeDto(val periode: PeriodeDto,
                                          val hendelsestype: Hendelsestype,
                                          val hendelsesundertype: Hendelsesundertype)


@JsonTypeName(BehandlingsstegForeldelseDto.STEG_NAVN)
data class BehandlingsstegForeldelseDto(val foreldetPerioder: List<ForeldelsesperiodeDto>) : BehandlingsstegDto() {

    override fun getSteg(): String {
        return STEG_NAVN
    }

    companion object {

        const val STEG_NAVN = "FORELDELSE"
    }
}

data class ForeldelsesperiodeDto(val periode: PeriodeDto,
                                 val begrunnelse: String,
                                 val foreldelsesvurderingstype: Foreldelsesvurderingstype,
                                 val foreldelsesfrist: LocalDate? = null,
                                 val oppdagelsesdato: LocalDate? = null)

