package no.nav.familie.tilbake.config

import no.nav.familie.tilbake.integration.økonomi.OppdragClient
import no.nav.familie.tilbake.kontrakter.simulering.FeilutbetalingerFraSimulering
import no.nav.familie.tilbake.kontrakter.simulering.HentFeilutbetalingerFraSimuleringRequest
import no.nav.familie.tilbake.log.SecureLog
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Primary
@Service
class OppdragClientMock : OppdragClient {
    override fun hentFeilutbetalingerFraSimulering(
        request: HentFeilutbetalingerFraSimuleringRequest,
        logContext: SecureLog.Context,
    ): FeilutbetalingerFraSimulering {
        TODO("Not yet implemented")
    }
}
