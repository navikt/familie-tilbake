package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.repository.tbd.Kravgrunnlag431Repository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = BehandleKravgrunnlagTask.BEHANDLE_KRAVGRUNNLAG,
                     maxAntallFeil = 3,
                     beskrivelse = "Håndter mottatt kravgrunnlag fra oppdrag",
                     triggerTidVedFeilISekunder = 60 * 5)
class BehandleKravgrunnlagTask(private val kravgrunnlag431Repository: Kravgrunnlag431Repository) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)
    private val secure_log = LoggerFactory.getLogger("secureLogger")

    override fun doTask(task: Task) {
        //TODO map fra kravgrunnlag xml i payload til domene + validering +++
        log.info("Behandler kravgrunnlag.")
    }

    companion object {

        const val BEHANDLE_KRAVGRUNNLAG = "behandleKravgrunnlag"
    }
}