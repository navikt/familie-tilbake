package no.nav.familie.tilbake.service.dokumentbestilling.innhentdokumentasjon

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmottager
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = InnhentDokumentasjonbrevTask.TYPE,
                     maxAntallFeil = 3,
                     beskrivelse = "Sender innhent dokumentasjonsbrev",
                     triggerTidVedFeilISekunder = 60 * 5)
class InnhentDokumentasjonbrevTask(val behandlingRepository: BehandlingRepository,
                                   val innhentDokumentasjonBrevService: InnhentDokumentasjonbrevService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fritekst: String = task.metadata.getProperty("fritekst")
        if (behandling.harVerge) {
            innhentDokumentasjonBrevService.sendInnhentDokumentasjonBrev(behandling, fritekst, Brevmottager.VERGE)
        }
        innhentDokumentasjonBrevService.sendInnhentDokumentasjonBrev(behandling, fritekst, Brevmottager.BRUKER)
    }

    companion object {

        const val TYPE = "brev.sendInnhentDokumentasjon"
    }

}
