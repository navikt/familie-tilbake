package no.nav.familie.tilbake.historikkinnslag

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.config.PropertyName
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
) : AsyncTaskStep {
    private val log = TracedLogger.getLogger<LagHistorikkinnslagTask>()

    override fun doTask(task: Task) {
        val behandlingId: UUID = UUID.fromString(task.payload)
        val logContext = logService.contextFraBehandling(behandlingId)
        log.medContext(logContext) {
            info("LagHistorikkinnslagTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        }

        val historikkinnslagstype =
            TilbakekrevingHistorikkinnslagstype.valueOf(task.metadata.getProperty("historikkinnslagstype"))
        val aktør = Aktør.valueOf(task.metadata.getProperty("aktør"))
        val opprettetTidspunkt = LocalDateTime.parse(task.metadata.getProperty("opprettetTidspunkt"))
        val beskrivelse = task.metadata.getProperty("beskrivelse")
        val brevtype = task.metadata.getProperty("brevtype")
        val beslutter =
            task.metadata.getProperty(PropertyName.BESLUTTER) ?: "TBD".takeIf {
                historikkinnslagstype == TilbakekrevingHistorikkinnslagstype.BEHANDLING_SENDT_TILBAKE_TIL_SAKSBEHANDLER &&
                    behandlingId.toString() in
                    setOf(
                        "529ba021-8a96-440c-831c-f3e847915f54",
                        "ab9501c1-6d83-4fea-8080-96c65f73459a",
                        "b072cc7f-b4b2-45c0-ba0c-4e24004e3758",
                        "e9be4a9d-66a1-45d6-92ef-7a46a38829a4",
                        "fed36879-5032-4b59-8cf6-03878a6c25f2",
                        "0e301b5b-0f96-42b1-9910-951882aeca1f",
                        "906b0aef-0297-4d4b-9492-c98a2dfa0c2c",
                    )
            }

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

    companion object {
        const val TYPE = "lagHistorikkinnslag"
    }
}
