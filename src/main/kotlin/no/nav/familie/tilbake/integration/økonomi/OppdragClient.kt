package no.nav.familie.tilbake.integration.økonomi

import AbstractPingableRestClient
import no.nav.familie.tilbake.common.exceptionhandler.IntegrasjonException
import no.nav.familie.tilbake.kontrakter.Ressurs
import no.nav.familie.tilbake.kontrakter.getDataOrThrow
import no.nav.familie.tilbake.kontrakter.simulering.FeilutbetalingerFraSimulering
import no.nav.familie.tilbake.kontrakter.simulering.FeilutbetaltPeriode
import no.nav.familie.tilbake.kontrakter.simulering.HentFeilutbetalingerFraSimuleringRequest
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import org.springframework.web.client.RestOperations
import org.springframework.web.util.UriComponentsBuilder
import java.math.BigDecimal
import java.net.URI
import java.time.YearMonth

interface OppdragClient {
    fun hentFeilutbetalingerFraSimulering(
        request: HentFeilutbetalingerFraSimuleringRequest,
        logContext: SecureLog.Context,
    ): FeilutbetalingerFraSimulering
}

@Service
@Profile("!e2e & !mock-økonomi")
class DefaultOppdragClient(
    @Qualifier("azure") restOperations: RestOperations,
    @Value("\${FAMILIE_OPPDRAG_URL}") private val familieOppdragUrl: URI,
) : AbstractPingableRestClient(restOperations, "familie.oppdrag"),
    OppdragClient {
    private val logger = TracedLogger.getLogger<DefaultOppdragClient>()

    private val hentFeilutbetalingerFraSimuleringUri: URI =
        UriComponentsBuilder
            .fromUri(familieOppdragUrl)
            .pathSegment(HENT_FEILUTBETALINGER_PATH)
            .build()
            .toUri()

    override fun hentFeilutbetalingerFraSimulering(
        request: HentFeilutbetalingerFraSimuleringRequest,
        logContext: SecureLog.Context,
    ): FeilutbetalingerFraSimulering {
        logger.medContext(logContext) {
            info(
                "Henter feilubetalinger fra simulering for ytelsestype=${request.ytelsestype}, " +
                    "eksternFagsakId=${request.eksternFagsakId} og eksternId=${request.fagsystemsbehandlingId}",
            )
        }
        try {
            return postForEntity<Ressurs<FeilutbetalingerFraSimulering>>(
                uri = hentFeilutbetalingerFraSimuleringUri,
                payload = request,
            ).getDataOrThrow()
        } catch (exception: Exception) {
            logger.medContext(logContext) {
                error(
                    "Feilutbetalinger kan ikke hentes fra simulering for for ytelsestype=${request.ytelsestype}, " +
                        "eksternFagsakId=${request.eksternFagsakId} og eksternId=${request.fagsystemsbehandlingId} " +
                        "Feiler med ${exception.message}",
                )
            }
            throw IntegrasjonException(
                msg = "Noe gikk galt ved henting av feilutbetalinger fra simulering",
                throwable = exception,
                logContext = logContext,
            )
        }
    }

    companion object {
        const val HENT_FEILUTBETALINGER_PATH = "api/simulering/feilutbetalinger"
    }
}

@Service
@Profile("e2e", "mock-økonomi")
class MockOppdragClient : OppdragClient {
    override fun hentFeilutbetalingerFraSimulering(
        request: HentFeilutbetalingerFraSimuleringRequest,
        logContext: SecureLog.Context,
    ): FeilutbetalingerFraSimulering {
        logger.medContext(logContext) {
            info(
                "Henter feilubetalinger fra simulering i e2e-profil for ytelsestype=${request.ytelsestype}, " +
                    "eksternFagsakId=${request.eksternFagsakId} og eksternId=${request.fagsystemsbehandlingId}",
            )
        }
        val feilutbetaltPeriode =
            FeilutbetaltPeriode(
                fom = YearMonth.now().minusMonths(2).atDay(1),
                tom = YearMonth.now().minusMonths(1).atDay(1),
                feilutbetaltBeløp = BigDecimal("20000"),
                tidligereUtbetaltBeløp = BigDecimal("30000"),
                nyttBeløp = BigDecimal("10000"),
            )
        return FeilutbetalingerFraSimulering(feilutbetaltePerioder = listOf(feilutbetaltPeriode))
    }

    companion object {
        private val logger = TracedLogger.getLogger<MockOppdragClient>()
    }
}
