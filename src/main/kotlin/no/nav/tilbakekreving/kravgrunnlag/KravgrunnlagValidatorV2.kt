package no.nav.tilbakekreving.kravgrunnlag

import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag.detalj.v1.DetaljertKravgrunnlagDto

class KravgrunnlagValidatorV2(
    private val kravgrunnlag: DetaljertKravgrunnlagDto,
    private val periodeValidator: PeriodeValidator,
) {
    private val feil = mutableListOf<ValidationResult.Feil.Failure>()

    fun valider(): ValidationResult {
        validerReferanse()
        validerPerioder()
        return when {
            feil.isEmpty() -> ValidationResult.Ok
            else -> ValidationResult.Feil(kravgrunnlag.kravgrunnlagId.toString(), feil)
        }
    }

    private fun validerReferanse() {
        failOn(kravgrunnlag.referanse == null) { "Mangler referanse" }
    }

    private fun validerPerioder() {
        val perioder = kravgrunnlag.tilbakekrevingsPeriode
            .map { it.periode.fom til it.periode.tom }
            .sortedBy { it.fom }

        perioder.forEach { feil.addAll(periodeValidator.valider(it)) }
        perioder
            .zipWithNext()
            .forEach { (nåværende, neste) ->
                failOn(neste.fom <= nåværende.tom) {
                    "Perioden ${nåværende.fom} til ${nåværende.tom} overlapper med perioden ${neste.fom} til ${neste.tom}"
                }
            }
    }

    private fun failOn(
        failureCondition: Boolean,
        message: () -> String,
    ) {
        if (failureCondition) {
            feil.add(ValidationResult.Feil.Failure(message()))
        }
    }

    companion object {
        fun valider(
            kravgrunnlag: DetaljertKravgrunnlagDto,
            periodeValidator: PeriodeValidator,
        ): ValidationResult {
            return KravgrunnlagValidatorV2(kravgrunnlag, periodeValidator)
                .valider()
        }
    }
}
