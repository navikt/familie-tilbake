package no.nav.tilbakekreving.api.v1.dto

import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnType
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import java.math.BigDecimal
import java.time.LocalDateTime

data class VurdertVilkårsvurderingDto(
    val perioder: List<VurdertVilkårsvurderingsperiodeDto>,
    val rettsgebyr: Long,
    val opprettetTid: LocalDateTime?,
)

data class VurdertVilkårsvurderingsperiodeDto(
    val periode: Datoperiode,
    val feilutbetaltBeløp: BigDecimal,
    val hendelsestype: Hendelsestype,
    val reduserteBeløper: List<RedusertBeløpDto> = listOf(),
    val aktiviteter: List<AktivitetDto> = listOf(),
    val vilkårsvurderingsresultatInfo: VurdertVilkårsvurderingsresultatDto? = null,
    val begrunnelse: String? = null,
    val foreldet: Boolean,
)

data class VurdertVilkårsvurderingsresultatDto(
    val vilkårsvurderingsresultat: Vilkårsvurderingsresultat? = null,
    val godTro: VurdertGodTroDto? = null,
    val aktsomhet: VurdertAktsomhetDto? = null,
)

data class VurdertGodTroDto(
    val beløpErIBehold: Boolean,
    val beløpTilbakekreves: BigDecimal? = null,
    val begrunnelse: String,
)

data class VurdertAktsomhetDto(
    val aktsomhet: Aktsomhet,
    val ileggRenter: Boolean? = null,
    val andelTilbakekreves: BigDecimal? = null,
    val beløpTilbakekreves: BigDecimal? = null,
    val begrunnelse: String,
    val særligeGrunner: List<VurdertSærligGrunnDto>? = null,
    val særligeGrunnerTilReduksjon: Boolean = false,
    val tilbakekrevSmåbeløp: Boolean = true,
    val særligeGrunnerBegrunnelse: String? = null,
)

data class VurdertSærligGrunnDto(
    val særligGrunn: SærligGrunnType,
    val begrunnelse: String? = null,
)

data class RedusertBeløpDto(
    val trekk: Boolean,
    val beløp: BigDecimal,
)

data class AktivitetDto(
    val aktivitet: String,
    val beløp: BigDecimal,
)
