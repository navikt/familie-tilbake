package no.nav.familie.tilbake.dokumentbestilling.felles.task

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.VarselService
import no.nav.familie.tilbake.log.SecureLog.Context.Companion.logContext
import no.nav.familie.tilbake.log.TracedLogger
import org.springframework.stereotype.Service
import java.util.Base64
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = LagreVarselbrevsporingTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Lagrer varselbrev",
    triggerTidVedFeilISekunder = 60 * 5L,
)
class LagreVarselbrevsporingTask(
    private val varselService: VarselService,
) : AsyncTaskStep {
    private val log = TracedLogger.getLogger<LagreVarselbrevsporingTask>()

    override fun doTask(task: Task) {
        log.medContext(task.logContext()) { info("{} prosesserer med id={}", this::class.simpleName, task.id) }

        val varseltekstBase64: String = task.metadata.getProperty("fritekst")
        val varseltekst = Base64.getDecoder().decode(varseltekstBase64).decodeToString()
        val varselbeløp: Long = task.metadata.getProperty("varselbeløp").toLong()
        val behandlingId = UUID.fromString(task.payload)
        varselService.lagre(behandlingId, varseltekst, varselbeløp)
    }

    companion object {
        const val TYPE = "lagreVarselbrevsporing"
    }
}
