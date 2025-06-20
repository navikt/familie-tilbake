package no.nav.familie.tilbake.behandling.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.HentFagsystemsbehandlingService
import no.nav.familie.tilbake.behandling.Ytelsestype
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = OppdaterFaktainfoTask.TYPE,
    beskrivelse = "oppdaterer fakta info når kravgrunnlag mottas av ny referanse",
    maxAntallFeil = 10,
    triggerTidVedFeilISekunder = 5L,
)
class OppdaterFaktainfoTask(
    private val hentFagsystemsbehandlingService: HentFagsystemsbehandlingService,
    private val behandlingService: BehandlingService,
) : AsyncTaskStep {
    private val log = TracedLogger.getLogger<OppdaterFaktainfoTask>()

    override fun doTask(task: Task) {
        val eksternFagsakId = task.metadata.getProperty("eksternFagsakId")
        val logContext = SecureLog.Context.utenBehandling(eksternFagsakId)
        log.medContext(logContext) {
            info("OppdaterFaktainfoTask prosesserer med id={} og metadata {}", task.id, task.metadata.toString())
        }
        val ytelsestype = Ytelsestype.valueOf(task.metadata.getProperty("ytelsestype"))
        val eksternId = task.metadata.getProperty("eksternId")

        val requestSendt =
            requireNotNull(
                hentFagsystemsbehandlingService.hentFagsystemsbehandlingRequestSendt(
                    eksternFagsakId,
                    ytelsestype,
                    eksternId,
                ),
            )
        // kaster exception inntil respons-en har mottatt
        val hentFagsystemsbehandlingRespons =
            requireNotNull(requestSendt.respons) {
                "HentFagsystemsbehandlingRespons er ikke mottatt fra fagsystem for " +
                    "eksternFagsakId=$eksternFagsakId,ytelsestype=$ytelsestype,eksternId=$eksternId." +
                    "Task kan kjøre på nytt manuelt når respons er mottatt."
            }

        val respons = hentFagsystemsbehandlingService.lesRespons(hentFagsystemsbehandlingRespons)
        val feilmelding = respons.feilMelding
        if (feilmelding != null) {
            throw Feil(
                message = "Noen gikk galt mens henter fagsystemsbehandling fra fagsystem. Feiler med $feilmelding",
                logContext = logContext,
            )
        }
        behandlingService.oppdaterFaktainfo(eksternFagsakId, ytelsestype, eksternId, respons.hentFagsystemsbehandling!!)
    }

    companion object {
        const val TYPE = "oppdater.faktainfo"
    }
}
