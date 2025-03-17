package no.nav.tilbakekreving.api.v1.dto

import java.math.BigDecimal
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vurdering

data class BeregningsresultatDto(
    val beregningsresultatsperioder: List<BeregningsresultatsperiodeDto>,
    val vedtaksresultat: Vedtaksresultat,
    val vurderingAvBrukersUttalelse: VurderingAvBrukersUttalelseDto,
)

data class BeregningsresultatsperiodeDto(
    val periode: Datoperiode,
    val vurdering: Vurdering? = null,
    val feilutbetaltBeløp: BigDecimal,
    val andelAvBeløp: BigDecimal? = null,
    val renteprosent: BigDecimal? = null,
    val tilbakekrevingsbeløp: BigDecimal? = null,
    val tilbakekrevesBeløpEtterSkatt: BigDecimal? = null,
)
