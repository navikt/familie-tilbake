package no.nav.familie.tilbake.dokumentbestilling.varsel

import no.nav.familie.kontrakter.felles.simulering.FeilutbetaltPeriode
import no.nav.familie.kontrakter.felles.simulering.HentFeilutbetalingerFraSimuleringRequest
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.integration.økonomi.OppdragClient
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class SimuleringService(private val oppdragClient: OppdragClient) {

    fun hentFeilutbetalingerFraSimulering(ytelsestype: Ytelsestype,
                                          eksternFagsakId: String,
                                          eksternId: String,
                                          varsletTotalFeilutbetaltBeløp: BigDecimal): List<FeilutbetaltPeriode> {
        if (ytelsestype !in listOf(Ytelsestype.OVERGANGSSTØNAD, Ytelsestype.BARNETILSYN)) {
            throw Feil(message = "Støtter ikke funskjonalitet for ytelsestype=$ytelsestype")
        }
        val request = HentFeilutbetalingerFraSimuleringRequest(ytelsestype = ytelsestype,
                                                               eksternFagsakId = eksternFagsakId,
                                                               fagsystemsbehandlingId = eksternId)
        val feilutbetalingerFraSimulering = oppdragClient.hentFeilutbetalingerFraSimulering(request)

        validateRespons(feilutbetalingerFraSimulering.feilutbetaltePerioder,
                        varsletTotalFeilutbetaltBeløp,
                        ytelsestype,
                        eksternFagsakId,
                        eksternId)
        return feilutbetalingerFraSimulering.feilutbetaltePerioder
    }

    private fun validateRespons(feilutbetaltePerioder: List<FeilutbetaltPeriode>,
                                varsletTotalFeilutbetaltBeløp: BigDecimal,
                                ytelsestype: Ytelsestype,
                                eksternFagsakId: String,
                                eksternId: String) {
        if (feilutbetaltePerioder.isEmpty() && varsletTotalFeilutbetaltBeløp > BigDecimal.ZERO) {
            throw Feil(message = "Kan ikke hente feilutbetalinger fra simulering for ytelsestype=$ytelsestype, " +
                       "eksternFagsakId=$eksternFagsakId og eksternId=$eksternId")
        }
        if (feilutbetaltePerioder.sumOf { it.feilutbetaltBeløp } != varsletTotalFeilutbetaltBeløp) {
            throw Feil(message = "Varslet totalFeilutbetaltBeløp matcher ikke med hentet totalFeilutbetaltBeløp fra simulering " +
                       "for ytelsestype=$ytelsestype, eksternFagsakId=$eksternFagsakId og eksternId=$eksternId")
        }
    }

}