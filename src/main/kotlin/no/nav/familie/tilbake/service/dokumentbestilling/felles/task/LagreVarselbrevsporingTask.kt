package no.nav.familie.tilbake.service.dokumentbestilling.felles.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.behandling.VarselService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = LagreVarselbrevsporingTask.TYPE,
                     maxAntallFeil = 3,
                     beskrivelse = "Lagrer varselbrev",
                     triggerTidVedFeilISekunder = 60 * 5)
class LagreVarselbrevsporingTask(private val varselService: VarselService) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("${this::class.simpleName} prosesserer med id=${task.id} og metadata ${task.metadata}")

        val varseltekst: String = task.metadata.getProperty("fritekst")
        val varselbeløp: Long = task.metadata.getProperty("varselbeløp").toLong()
        varselService.lagre(task.payload, varseltekst, varselbeløp)
    }

    companion object {

        const val TYPE = "lagreVarselbrevsporing"
    }

}