package no.nav.familie.tilbake.service.beregning

import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.domain.tbd.Vurdering
import java.math.BigDecimal

data class BeregningResultatPeriode(val periode: Periode,
                                    val vurdering: Vurdering,
                                    val feilutbetaltBeløp: BigDecimal = BigDecimal.ZERO,
                                    val andelAvBeløp: BigDecimal?,
                                    val renterProsent: BigDecimal = BigDecimal.ZERO,
                                    val manueltSattTilbakekrevingsbeløp: BigDecimal? = null,
                                    val tilbakekrevingBeløpUtenRenter: BigDecimal = BigDecimal.ZERO,
                                    val renteBeløp: BigDecimal = BigDecimal.ZERO,
                                    val tilbakekrevingBeløp: BigDecimal = BigDecimal.ZERO,
                                    val skattBeløp: BigDecimal = BigDecimal.ZERO,
                                    val tilbakekrevingBeløpEtterSkatt: BigDecimal = BigDecimal.ZERO,
        //rått beløp, ikke justert for evt. trekk
                                    val utbetaltYtelseBeløp: BigDecimal = BigDecimal.ZERO,
        //rått beløp, ikke justert for evt. trekk
                                    val riktigYtelseBeløp: BigDecimal = BigDecimal.ZERO)