package no.nav.tilbakekreving.kravgrunnlag

import no.nav.familie.tilbake.common.exceptionhandler.UgyldigKravgrunnlagFeil
import no.nav.familie.tilbake.log.SecureLog

sealed interface ValidationResult {
    fun throwOnError(logContext: SecureLog.Context)

    data object Ok : ValidationResult {
        override fun throwOnError(logContext: SecureLog.Context) {}
    }

    class Feil(
        private val kravgrunnlagId: String,
        val failures: List<Failure>,
    ) : ValidationResult {
        override fun throwOnError(logContext: SecureLog.Context) {
            throw UgyldigKravgrunnlagFeil(
                melding = "Ugyldig kravgrunnlag for kravgrunnlagId $kravgrunnlagId. ${failures.first().melding}.",
                logContext = logContext,
            )
        }

        class Failure(val melding: String)
    }
}
