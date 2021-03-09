package no.nav.familie.tilbake.kravgrunnlag.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = BehandleKravgrunnlagTask.BEHANDLE_KRAVGRUNNLAG,
                     maxAntallFeil = 3,
                     beskrivelse = "Håndter mottatt kravgrunnlag fra oppdrag",
                     triggerTidVedFeilISekunder = 60 * 5)
class BehandleKravgrunnlagTask(private val kravgrunnlagService: KravgrunnlagService) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)
    private val secureLog = LoggerFactory.getLogger("secureLogger")

    override fun doTask(task: Task) {
        log.info("BehandleKravgrunnlagTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        kravgrunnlagService.håndterMottattKravgrunnlag(task.payload)
    }

    companion object {

        const val BEHANDLE_KRAVGRUNNLAG = "behandleKravgrunnlag"
    }
}
