package no.nav.familie.tilbake.behandling.batch

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.Fagsystem
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagService
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.kontrakter.behandling.Saksbehandlingstype
import org.springframework.stereotype.Service
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = AutomatiskSaksbehandlingTask.TYPE,
    beskrivelse = "behandler behandling automatisk",
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = 60 * 5L,
)
class AutomatiskSaksbehandlingTask(
    private val automatiskSaksbehandlingService: AutomatiskSaksbehandlingService,
    private val kravgrunnlagService: KravgrunnlagService,
    private val behandlingRepository: BehandlingRepository,
    private val logService: LogService,
) : AsyncTaskStep {
    private val log = TracedLogger.getLogger<AutomatiskSaksbehandlingTask>()

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val logContext = logService.contextFraBehandling(behandling.id)
        log.medContext(logContext) {
            info("AutomatiskSaksbehandlingTask prosesserer med id={} og metadata {}", task.id, task.metadata.toString())
        }
        validerOmAutomatiskBehandlingUnder4RettsgebyrErMulig(behandling, logContext)
        automatiskSaksbehandlingService.settSaksbehandlingstypeTilAutomatiskHvisOrdinær(behandlingId)

        automatiskSaksbehandlingService.behandleAutomatisk(behandlingId, logContext)
    }

    private fun validerOmAutomatiskBehandlingUnder4RettsgebyrErMulig(
        behandling: Behandling,
        logContext: SecureLog.Context,
    ) {
        if (kravgrunnlagService.erOverFireRettsgebyr(behandling) &&
            behandling.saksbehandlingstype == Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_UNDER_4X_RETTSGEBYR
        ) {
            throw Feil(
                message = "Skal ikke behandle beløp over 4x rettsgebyr automatisk",
                logContext = logContext,
            )
        }
    }

    companion object {
        fun opprettTask(
            behandlingId: UUID,
            fagsystem: Fagsystem,
        ): Task =
            Task(
                type = TYPE,
                payload = behandlingId.toString(),
                properties =
                    Properties().apply {
                        setProperty(
                            PropertyName.FAGSYSTEM,
                            fagsystem.name,
                        )
                    },
            )

        const val TYPE = "saksbehandle.automatisk"
    }
}
