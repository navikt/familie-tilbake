package no.nav.familie.tilbake.brev.modell

import no.nav.familie.tilbake.common.Periode
import java.math.BigDecimal
import java.time.LocalDate

class LogiskPeriode(val periode: Periode,
                    val feilutbetaltBeløp: BigDecimal) {

    val fom: LocalDate = periode.fom
    val tom: LocalDate = periode.tom
}
