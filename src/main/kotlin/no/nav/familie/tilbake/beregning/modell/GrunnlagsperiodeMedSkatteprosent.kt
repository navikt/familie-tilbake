package no.nav.familie.tilbake.beregning.modell

import no.nav.tilbakekreving.kontrakter.Månedsperiode
import java.math.BigDecimal

class GrunnlagsperiodeMedSkatteprosent(
    val periode: Månedsperiode,
    val tilbakekrevingsbeløp: BigDecimal,
    val skatteprosent: BigDecimal,
)
