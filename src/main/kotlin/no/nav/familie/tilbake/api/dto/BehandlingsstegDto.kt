package no.nav.familie.tilbake.api.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.SærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import java.math.BigDecimal
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(JsonSubTypes.Type(value = BehandlingsstegFaktaDto::class),
              JsonSubTypes.Type(value = BehandlingsstegForeldelseDto::class),
              JsonSubTypes.Type(value = BehandlingsstegVilkårsvurderingDto::class))
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


@JsonTypeName(BehandlingsstegVilkårsvurderingDto.STEG_NAVN)
data class BehandlingsstegVilkårsvurderingDto(val vilkårsvurderingsperioder: List<VilkårsvurderingsperiodeDto>)
    : BehandlingsstegDto() {

    override fun getSteg(): String {
        return STEG_NAVN
    }

    companion object {

        const val STEG_NAVN = "VILKÅRSVURDERING"
    }
}

data class VilkårsvurderingsperiodeDto(
        val periode: PeriodeDto,
        val vilkårsvurderingsresultat: Vilkårsvurderingsresultat,
        val begrunnelse: String,
        val godTroDto: GodTroDto? = null,
        val aktsomhetDto: AktsomhetDto? = null)

data class GodTroDto(val beløpErIBehold: Boolean,
                     val beløpTilbakekreves: BigDecimal? = null,
                     val begrunnelse: String)

data class AktsomhetDto(val aktsomhet: Aktsomhet,
                        val ileggRenter: Boolean? = null,
                        val andelTilbakekreves: BigDecimal? = null,
                        val beløpTilbakekreves: BigDecimal? = null,
                        val begrunnelse: String,
                        val særligeGrunner: List<SærligGrunnDto>? = null,
                        val særligeGrunnerTilReduksjon: Boolean = false,
                        val tilbakekrevSmåbeløp: Boolean = true,
                        val særligeGrunnerBegrunnelse: String? = null)

data class SærligGrunnDto(val særligGrunn: SærligGrunn,
                          val begrunnelse: String? = null)
