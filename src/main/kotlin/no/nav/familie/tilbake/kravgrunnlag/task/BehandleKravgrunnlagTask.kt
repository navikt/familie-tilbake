package no.nav.familie.tilbake.kravgrunnlag.task

import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagService
import no.nav.familie.tilbake.log.SecureLog.Context.Companion.logContext
import no.nav.familie.tilbake.micrometer.AsyncTaskStepMedMetrics
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandleKravgrunnlagTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Håndter mottatt kravgrunnlag fra oppdrag",
    triggerTidVedFeilISekunder = 60 * 5L,
)
class BehandleKravgrunnlagTask(
    private val kravgrunnlagService: KravgrunnlagService,
) : AsyncTaskStepMedMetrics() {
    override fun doTaskWithMetrics(task: Task) {
        kravgrunnlagService.håndterMottattKravgrunnlag(task.payload, task.id, task.metadata, task.logContext())
    }

    companion object {
        const val TYPE = "behandleKravgrunnlag"
    }
}
