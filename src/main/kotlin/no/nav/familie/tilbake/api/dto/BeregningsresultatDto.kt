package no.nav.familie.tilbake.api.dto

import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vurdering
import no.nav.tilbakekreving.kontrakter.Datoperiode
import java.math.BigDecimal

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
