package no.nav.familie.tilbake.api.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.SærligGrunn
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import java.math.BigDecimal
import java.time.LocalDate
import javax.validation.Valid
import javax.validation.constraints.Size

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(JsonSubTypes.Type(value = BehandlingsstegFaktaDto::class),
              JsonSubTypes.Type(value = BehandlingsstegForeldelseDto::class),
              JsonSubTypes.Type(value = BehandlingsstegVilkårsvurderingDto::class),
              JsonSubTypes.Type(value = BehandlingsstegForeslåVedtaksstegDto::class),
              JsonSubTypes.Type(value = BehandlingsstegFatteVedtaksstegDto::class))
abstract class BehandlingsstegDto protected constructor() {

    abstract fun getSteg(): String
}

@JsonTypeName(BehandlingsstegFaktaDto.STEGNAVN)
data class BehandlingsstegFaktaDto(val feilutbetaltePerioder: List<FaktaFeilutbetalingsperiodeDto>,
                                   @Size(max = 1500, message = "begrunnelse er for lang")
                                   val begrunnelse: String) : BehandlingsstegDto() {

    override fun getSteg(): String {
        return STEGNAVN
    }

    companion object {

        const val STEGNAVN = "FAKTA"
    }
}

data class FaktaFeilutbetalingsperiodeDto(val periode: PeriodeDto,
                                          val hendelsestype: Hendelsestype,
                                          val hendelsesundertype: Hendelsesundertype)


@JsonTypeName(BehandlingsstegForeldelseDto.STEGNAVN)
data class BehandlingsstegForeldelseDto(val foreldetPerioder: List<ForeldelsesperiodeDto>) : BehandlingsstegDto() {

    override fun getSteg(): String {
        return STEGNAVN
    }

    companion object {

        const val STEGNAVN = "FORELDELSE"
    }
}

data class ForeldelsesperiodeDto(val periode: PeriodeDto,
                                 @Size(max = 1500, message = "begrunnelse er for lang")
                                 val begrunnelse: String,
                                 val foreldelsesvurderingstype: Foreldelsesvurderingstype,
                                 val foreldelsesfrist: LocalDate? = null,
                                 val oppdagelsesdato: LocalDate? = null)


@JsonTypeName(BehandlingsstegVilkårsvurderingDto.STEGNAVN)
data class BehandlingsstegVilkårsvurderingDto(val vilkårsvurderingsperioder: List<VilkårsvurderingsperiodeDto>)
    : BehandlingsstegDto() {

    override fun getSteg(): String {
        return STEGNAVN
    }

    companion object {

        const val STEGNAVN = "VILKÅRSVURDERING"
    }
}

data class VilkårsvurderingsperiodeDto(val periode: PeriodeDto,
                                       val vilkårsvurderingsresultat: Vilkårsvurderingsresultat,
                                       @Size(max = 1500, message = "begrunnelse er for lang")
                                       val begrunnelse: String,
                                       val godTroDto: GodTroDto? = null,
                                       val aktsomhetDto: AktsomhetDto? = null)

data class GodTroDto(val beløpErIBehold: Boolean,
                     val beløpTilbakekreves: BigDecimal? = null,
                     @Size(max = 1500, message = "begrunnelse er for lang")
                     val begrunnelse: String)

data class AktsomhetDto(val aktsomhet: Aktsomhet,
                        val ileggRenter: Boolean? = null,
                        val andelTilbakekreves: BigDecimal? = null,
                        val beløpTilbakekreves: BigDecimal? = null,
                        @Size(max = 1500, message = "begrunnelse er for lang")
                        val begrunnelse: String,
                        val særligeGrunner: List<SærligGrunnDto>? = null,
                        val særligeGrunnerTilReduksjon: Boolean = false,
                        val tilbakekrevSmåbeløp: Boolean = true,
                        val særligeGrunnerBegrunnelse: String? = null)

data class SærligGrunnDto(val særligGrunn: SærligGrunn,
                          @Size(max = 1500, message = "begrunnelse er for lang")
                          val begrunnelse: String? = null)

@JsonTypeName(BehandlingsstegForeslåVedtaksstegDto.STEGNAVN)
data class BehandlingsstegForeslåVedtaksstegDto(val fritekstavsnitt: FritekstavsnittDto) : BehandlingsstegDto() {

    override fun getSteg(): String {
        return STEGNAVN
    }

    companion object {

        const val STEGNAVN = "FORESLÅ_VEDTAK"
    }
}

data class FritekstavsnittDto(@Size(max = 10000, message = "Oppsummeringstekst er for lang")
                              var oppsummeringstekst: String? = null,
                              @Size(max = 100, message = "For mange perioder")
                              @Valid
                              var perioderMedTekst: List<PeriodeMedTekstDto>)

@JsonTypeName(BehandlingsstegFatteVedtaksstegDto.STEGNAVN)
data class BehandlingsstegFatteVedtaksstegDto(val totrinnsvurderinger: List<VurdertTotrinnDto>) : BehandlingsstegDto() {

    override fun getSteg(): String {
        return STEGNAVN
    }

    companion object {

        const val STEGNAVN = "FATTE_VEDTAK"
    }
}

data class VurdertTotrinnDto(val behandlingssteg: Behandlingssteg,
                             val godkjent: Boolean,
                             @Size(max = 2000, message = "begrunnelse er for lang")
                             val begrunnelse: String? = null)
