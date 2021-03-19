package no.nav.familie.tilbake.beregning.modell

import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.domain.tbd.Vurdering
import java.math.BigDecimal

data class BeregningResultatPeriode(val periode: Periode,
                                    val vurdering: Vurdering? = null,
                                    val feilutbetaltBeløp: BigDecimal,
                                    val andelAvBeløp: BigDecimal? = null,
                                    val renterProsent: BigDecimal? = null,
                                    val manueltSattTilbakekrevingsbeløp: BigDecimal? = null,
                                    val tilbakekrevingBeløpUtenRenter: BigDecimal,
                                    val renteBeløp: BigDecimal? = null,
                                    val tilbakekrevingBeløp: BigDecimal,
                                    val skattBeløp: BigDecimal? = null,
                                    val tilbakekrevingBeløpEtterSkatt: BigDecimal? = null,
                                    val utbetaltYtelseBeløp: BigDecimal? = null,//rått beløp, ikke justert for evt. trekk
                                    val riktigYtelseBeløp: BigDecimal? = null) //rått beløp, ikke justert for evt. trekk

