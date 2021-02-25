package no.nav.familie.tilbake.service.beregning

import no.nav.familie.tilbake.common.Periode
import java.math.BigDecimal

internal class GrunnlagPeriodeMedSkattProsent(val periode: Periode,
                                              val tilbakekrevesBeløp: BigDecimal,
                                              val skattProsent: BigDecimal)