package no.nav.familie.tilbake.service.dokumentbestilling.innhentdokumentasjon

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmottager
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = InnhentDokumentasjonbrevTask.TYPE,
                     maxAntallFeil = 3,
                     beskrivelse = "Sender innhent dokumentasjonsbrev",
                     triggerTidVedFeilISekunder = 60 * 5)
class InnhentDokumentasjonbrevTask(val behandlingRepository: BehandlingRepository,
                                   val innhentDokumentasjonBrevService: InnhentDokumentasjonbrevService,
                                   val behandlingskontrollService: BehandlingskontrollService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fritekst: String = task.metadata.getProperty("fritekst")
        if (behandling.harVerge) {
            innhentDokumentasjonBrevService.sendInnhentDokumentasjonBrev(behandling, fritekst, Brevmottager.VERGE)
        }
        innhentDokumentasjonBrevService.sendInnhentDokumentasjonBrev(behandling, fritekst, Brevmottager.BRUKER)
        val tidsfrist = LocalDate.now().plus(Constants.brukersSvarfrist).plusDays(1)
        behandlingskontrollService.settBehandlingPåVent(behandlingId,
                                                         Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                                                         tidsfrist)
    }

    companion object {

        const val TYPE = "brev.sendInnhentDokumentasjon"
    }

}