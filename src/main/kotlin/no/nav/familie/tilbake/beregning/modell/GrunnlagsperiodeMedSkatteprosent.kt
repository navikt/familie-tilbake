package no.nav.familie.tilbake.beregning.modell

import no.nav.familie.tilbake.common.Periode
import java.math.BigDecimal

class GrunnlagsperiodeMedSkatteprosent(
    val periode: Periode,
    val tilbakekrevingsbeløp: BigDecimal,
    val skatteprosent: BigDecimal
)
