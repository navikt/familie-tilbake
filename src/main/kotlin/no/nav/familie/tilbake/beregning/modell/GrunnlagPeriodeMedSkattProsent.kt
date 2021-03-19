package no.nav.familie.tilbake.beregning.modell

import no.nav.familie.tilbake.common.Periode
import java.math.BigDecimal

class GrunnlagPeriodeMedSkattProsent(val periode: Periode,
                                     val tilbakekrevesBeløp: BigDecimal,
                                     val skattProsent: BigDecimal)
