package no.nav.familie.tilbake.proxy.aInntekt

import kotlinx.coroutines.runBlocking
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class ArbeidOgInntektService(
    private val client: ArbeidOgInntektClient,
) {
    private val logger = TracedLogger.getLogger<ArbeidOgInntektClient>()

    fun hentAInntektUrl(
        personIdent: String,
        fagsakId: String?,
        behandlingId: String?,
    ): String {
        val logContext = SecureLog.Context.medBehandling(fagsakId, behandlingId)
        return runBlocking {
            try {
                val response = client.hentAInntektUrl(personIdent)
                logger.medContext(logContext) {
                    info("Hentet a-inntekt url")
                }
                response
            } catch (e: Exception) {
                logger.medContext(logContext) {
                    error("Feil ved henting av a-inntekt url fra Arbeid og Inntekt. Se Securelogs for detaljer.", e)
                }
                val feilmelding = "Feil ved henting av a-inntekt url fra Arbeid og Inntekt"
                throw Feil(
                    message = feilmelding,
                    logContext = logContext,
                    frontendFeilmelding = feilmelding,
                    httpStatus = HttpStatus.BAD_GATEWAY,
                )
            }
        }
    }
}
