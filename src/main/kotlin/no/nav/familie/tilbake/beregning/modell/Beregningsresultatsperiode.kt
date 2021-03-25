package no.nav.familie.tilbake.beregning.modell

import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.domain.tbd.Vurdering
import java.math.BigDecimal

data class Beregningsresultatsperiode(val periode: Periode,
                                      val vurdering: Vurdering? = null,
                                      val feilutbetaltBeløp: BigDecimal,
                                      val andelAvBeløp: BigDecimal? = null,
                                      val renteprosent: BigDecimal? = null,
                                      val manueltSattTilbakekrevingsbeløp: BigDecimal? = null,
                                      val tilbakekrevingsbeløpUtenRenter: BigDecimal,
                                      val rentebeløp: BigDecimal? = null,
                                      val tilbakekrevingsbeløp: BigDecimal,
                                      val skattebeløp: BigDecimal? = null,
                                      val tilbakekrevingsbeløpEtterSkatt: BigDecimal? = null,
                                      val utbetaltYtelsesbeløp: BigDecimal? = null,//rått beløp, ikke justert for evt. trekk
                                      val riktigYtelsesbeløp: BigDecimal? = null) //rått beløp, ikke justert for evt. trekk

