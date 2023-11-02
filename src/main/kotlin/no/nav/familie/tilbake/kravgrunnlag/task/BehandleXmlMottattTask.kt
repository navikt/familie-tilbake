package no.nav.familie.tilbake.kravgrunnlag.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagService
import no.nav.familie.tilbake.kravgrunnlag.ØkonomiXmlMottattRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(
    taskStepType = BehandleXmlMottattTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Håndter mottatt kravgrunnlag fra oppdrag",
    triggerTidVedFeilISekunder = 60 * 5L,
)
class BehandleXmlMottattTask(private val xmlMottattRepository: ØkonomiXmlMottattRepository, private val kravgrunnlagService: KravgrunnlagService) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)
    private val secureLog = LoggerFactory.getLogger("secureLogger")

    override fun doTask(task: Task) {
        log.info("BehandleXmlMottattTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        secureLog.info("BehandleXmlMottattTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        kravgrunnlagService.behandleMottattKravgrunnlag(task.payload)
    }

    companion object {

        const val TYPE = "behandleXmlMottatt"
    }
}
