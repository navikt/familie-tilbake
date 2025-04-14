package no.nav.tilbakekreving.beregning.modell

import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.math.BigDecimal

class GrunnlagsperiodeMedSkatteprosent(
    val periode: Datoperiode,
    val tilbakekrevingsbeløp: BigDecimal,
    val skatteprosent: BigDecimal,
)
