package no.nav.tilbakekreving.kravgrunnlag

import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.time.YearMonth

sealed interface PeriodeValidator {
    fun valider(periode: Datoperiode): List<ValidationResult.Feil.Failure>

    object MånedsperiodeValidator : PeriodeValidator {
        override fun valider(periode: Datoperiode): List<ValidationResult.Feil.Failure> {
            val feil = mutableListOf<ValidationResult.Feil.Failure>()
            if (!validerMåned(periode)) {
                feil.add(ValidationResult.Feil.Failure("Perioden ${periode.fom} til ${periode.tom} er ikke innenfor samme kalendermåned"))
            }

            if (periode.fom.dayOfMonth != 1) {
                feil.add(ValidationResult.Feil.Failure("Perioden ${periode.fom} til ${periode.tom} starter ikke første dag i måned"))
            }

            if (periode.tom.dayOfMonth != periode.tom.lengthOfMonth()) {
                feil.add(ValidationResult.Feil.Failure("Perioden ${periode.fom} til ${periode.tom} slutter ikke siste dag i måned"))
            }

            return feil
        }

        private fun validerMåned(periode: Datoperiode): Boolean {
            val fomMonth = YearMonth.from(periode.fom)
            val tomMonth = YearMonth.from(periode.tom)
            return fomMonth == tomMonth
        }
    }
}
