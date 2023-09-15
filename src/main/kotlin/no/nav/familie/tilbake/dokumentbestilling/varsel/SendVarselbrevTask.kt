package no.nav.familie.tilbake.dokumentbestilling.varsel

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.FeatureToggleConfig
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = SendVarselbrevTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Sender varselbrev",
    triggerTidVedFeilISekunder = 60 * 5L,
)
class SendVarselbrevTask(
    private val varselbrevService: VarselbrevService,
    private val behandlingRepository: BehandlingRepository,
    private val fagsakRepository: FagsakRepository,
    private val featureToggleService: FeatureToggleService,
) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (featureToggleService.isEnabled(FeatureToggleConfig.KONSOLIDERT_HÅNDTERING_AV_BREVMOTTAKERE)) {
            varselbrevService.sendVarselbrev(behandling)
        } else {
            if (behandling.harVerge) {
                varselbrevService.sendVarselbrev(behandling, Brevmottager.VERGE)
            }
            val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
            val brevmottager = if (fagsak.institusjon != null) Brevmottager.INSTITUSJON else Brevmottager.BRUKER
            varselbrevService.sendVarselbrev(behandling, brevmottager)
        }
    }

    companion object {

        const val TYPE = "brev.sendVarsel"
    }
}
