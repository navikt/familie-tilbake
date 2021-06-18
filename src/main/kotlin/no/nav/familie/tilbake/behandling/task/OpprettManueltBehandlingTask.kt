package no.nav.familie.tilbake.behandling.task

import no.nav.familie.prosessering.TaskStepBeskrivelse
import org.springframework.stereotype.Service

@Service
@TaskStepBeskrivelse(taskStepType = OpprettManueltBehandlingTask.TYPE,
                     beskrivelse = "oppretter behandling manuelt",
                     triggerTidVedFeilISekunder = 60 * 5)
class OpprettManueltBehandlingTask {

    companion object {

        const val TYPE = "opprettManueltBehandling"
    }
}
