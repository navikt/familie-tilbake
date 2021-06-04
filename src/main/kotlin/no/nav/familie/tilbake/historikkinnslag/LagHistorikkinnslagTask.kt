package no.nav.familie.tilbake.historikkinnslag

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = LagHistorikkinnslagTask.TYPE,
                     maxAntallFeil = 3,
                     beskrivelse = "Lag historikkinnslag og sender det til kafka",
                     triggerTidVedFeilISekunder = 60 * 5)
class LagHistorikkinnslagTask(private val historikkService: HistorikkService) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("LagHistorikkinnslagTask prosesserer med id=${task.id} og metadata ${task.metadata}")

        val behandlingId: UUID = UUID.fromString(task.payload)
        val historikkinnslagstype =
                TilbakekrevingHistorikkinnslagstype.valueOf(task.metadata.getProperty("historikkinnslagstype"))
        val aktør = Aktør.valueOf(task.metadata.getProperty("aktor"))
        val begrunnelse = task.metadata.getProperty("begrunnelse")

        historikkService.lagHistorikkinnslag(behandlingId, historikkinnslagstype, aktør, begrunnelse)
    }

    companion object {

        const val TYPE = "lagHistorikkinnslag"
    }
}
