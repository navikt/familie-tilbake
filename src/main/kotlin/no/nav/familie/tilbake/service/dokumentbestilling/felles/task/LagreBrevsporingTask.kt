package no.nav.familie.tilbake.service.dokumentbestilling.felles.task

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmottager
import no.nav.familie.tilbake.service.dokumentbestilling.felles.domain.Brevtype
import no.nav.familie.tilbake.service.dokumentbestilling.felles.pdf.BrevsporingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = LagreBrevsporingTask.TYPE,
                     maxAntallFeil = 3,
                     beskrivelse = "Lagrer brev",
                     triggerTidVedFeilISekunder = 60 * 5)
class LagreBrevsporingTask(val brevsporingService: BrevsporingService,
                           private val taskService: TaskService,
                           private val historikkTaskService: HistorikkTaskService) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("${this::class.simpleName} prosesserer med id=${task.id} og metadata ${task.metadata}")
        val dokumentId = task.metadata.getProperty("dokumentId")
        val journalpostId = task.metadata.getProperty("journalpostId")
        val brevtype = Brevtype.valueOf(task.metadata.getProperty("brevtype"))


        brevsporingService.lagreInfoOmUtsendtBrev(UUID.fromString(task.payload),
                                                  dokumentId,
                                                  journalpostId,
                                                  brevtype)
    }

    override fun onCompletion(task: Task) {
        val mottager = Brevmottager.valueOf(task.metadata.getProperty("mottager"))
        val brevtype = Brevtype.valueOf(task.metadata.getProperty("brevtype"))
        val ansvarligSaksbehandler = task.metadata.getProperty("ansvarligSaksbehandler")

        historikkTaskService.lagHistorikkTask(behandlingId = UUID.fromString(task.payload),
                                              historikkinnslagstype = utledHistorikkinnslagType(brevtype),
                                              aktør = utledAktør(brevtype, ansvarligSaksbehandler))

        if (brevtype.gjelderVarsel() && mottager == Brevmottager.BRUKER) {
            taskService.save(Task(LagreVarselbrevsporingTask.TYPE, task.payload, task.metadata))
        }
    }

    private fun utledHistorikkinnslagType(brevtype: Brevtype): TilbakekrevingHistorikkinnslagstype {
        return when (brevtype) {
            Brevtype.VARSEL -> TilbakekrevingHistorikkinnslagstype.VARSELBREV_SENDT
            Brevtype.KORRIGERT_VARSEL -> TilbakekrevingHistorikkinnslagstype.KORRIGERT_VARSELBREV_SENDT
            Brevtype.INNHENT_DOKUMENTASJON -> TilbakekrevingHistorikkinnslagstype.INNHENT_DOKUMENTASJON_BREV_SENDT
            Brevtype.HENLEGGELSE -> TilbakekrevingHistorikkinnslagstype.HENLEGGELSESBREV_SENDT
            Brevtype.VEDTAK -> TilbakekrevingHistorikkinnslagstype.VEDTAKSBREV_SENDT
        }
    }

    private fun utledAktør(brevtype: Brevtype,
                           ansvarligSaksbehandler: String?): Aktør {
        return when {
            brevtype == Brevtype.INNHENT_DOKUMENTASJON -> Aktør.SAKSBEHANDLER
            brevtype == Brevtype.KORRIGERT_VARSEL -> Aktør.SAKSBEHANDLER
            ansvarligSaksbehandler != null && ansvarligSaksbehandler != "VL" -> Aktør.SAKSBEHANDLER
            else -> Aktør.VEDTAKSLØSNING
        }
    }

    companion object {

        const val TYPE = "lagreBrevsporing"
    }
}
