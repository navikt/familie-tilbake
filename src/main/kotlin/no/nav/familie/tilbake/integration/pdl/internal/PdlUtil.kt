package no.nav.familie.tilbake.integration.pdl.internal

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.log.SecureLog
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val logger: Logger = LoggerFactory.getLogger("PdlUtil")

inline fun <reified T : Any> feilsjekkOgReturnerData(
    pdlResponse: PdlBolkResponse<T>,
    logContext: SecureLog.Context,
): Map<String, T> {
    if (pdlResponse.data == null) {
        SecureLog.medContext(logContext) {
            error(
                "Data fra pdl er null ved bolkoppslag av {} fra PDL: {}",
                T::class.toString(),
                pdlResponse.errorMessages(),
            )
        }
        throw Feil(
            message = "Data er null fra PDL -  ${T::class}. Se secure logg for detaljer.",
            logContext = logContext,
        )
    }

    val feil =
        pdlResponse.data.personBolk
            .filter { it.code != "ok" }
            .associate { it.ident to it.code }
    if (feil.isNotEmpty()) {
        SecureLog.medContext(logContext) {
            error("Feil ved henting av {} fra PDL: {}", T::class.toString(), feil)
        }
        throw Feil(
            message = "Feil ved henting av ${T::class} fra PDL. Se secure logg for detaljer.",
            logContext = logContext,
        )
    }
    if (pdlResponse.harAdvarsel()) {
        logger.warn("Advarsel ved henting av {} fra PDL. Se securelogs for detaljer.", T::class.toString())
        SecureLog.medContext(logContext) {
            warn(
                "Advarsel ved henting av {} fra PDL: {}",
                T::class.toString(),
                pdlResponse.extensions?.warnings?.joinToString(","),
            )
        }
    }
    return pdlResponse.data.personBolk.associateBy({ it.ident }, { it.person!! })
}
