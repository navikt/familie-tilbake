package no.nav.familie.tilbake.historikkinnslag

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.TracedLogger
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = LagHistorikkinnslagTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Lag historikkinnslag og sender det til kafka",
    triggerTidVedFeilISekunder = 60 * 5L,
)
class LagHistorikkinnslagTask(
    private val historikkService: HistorikkService,
    private val logService: LogService,
    private val behandlingRepository: BehandlingRepository,
) : AsyncTaskStep {
    private val log = TracedLogger.getLogger<LagHistorikkinnslagTask>()

    override fun doTask(task: Task) {
        val behandlingId: UUID = UUID.fromString(task.payload)
        val logContext = logService.contextFraBehandling(behandlingId)
        log.medContext(logContext) {
            info("LagHistorikkinnslagTask prosesserer med id={} og metadata {}", task.id, task.metadata.toString())
        }

        val historikkinnslagstype =
            TilbakekrevingHistorikkinnslagstype.valueOf(task.metadata.getProperty("historikkinnslagstype"))
        val beslutter = task.metadata.getProperty(PropertyName.BESLUTTER)
        val aktør = hentAktørIdent(behandlingId, task.metadata.getProperty("aktør"), beslutter)
        val opprettetTidspunkt = LocalDateTime.parse(task.metadata.getProperty("opprettetTidspunkt"))
        val beskrivelse = task.metadata.getProperty("beskrivelse")
        val brevtype = task.metadata.getProperty("brevtype")?.let(Brevtype::valueOf)

        historikkService.lagHistorikkinnslag(
            behandlingId,
            historikkinnslagstype,
            aktør,
            opprettetTidspunkt,
            beskrivelse,
            brevtype,
            beslutter,
        )
    }

    private fun hentAktørIdent(
        behandlingId: UUID,
        aktørType: String,
        beslutter: String?,
    ): Aktør =
        when (aktørType) {
            "VEDTAKSLØSNING" -> Aktør.Vedtaksløsning
            "SAKSBEHANDLER" -> Aktør.Saksbehandler.fraBehandling(behandlingId, behandlingRepository)
            "BESLUTTER" ->
                Aktør.Beslutter(
                    beslutter
                        ?: behandlingRepository.findByIdOrThrow(behandlingId).ansvarligBeslutter
                        ?: error("Beslutter mangler ident for behandling: $behandlingId"),
                )
            else -> error("Ukjent aktør type $aktørType")
        }

    companion object {
        const val TYPE = "lagHistorikkinnslag"
    }
}
