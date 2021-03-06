package no.nav.familie.tilbake.dokumentbestilling.varsel.manuelt

import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.dokumentbestilling.brevmaler.Dokumentmalstype
import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmottager
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
@TaskStepBeskrivelse(taskStepType = SendManueltVarselbrevTask.TYPE,
                     maxAntallFeil = 3,
                     beskrivelse = "Sender manuelt varselbrev",
                     triggerTidVedFeilISekunder = 60 * 5)
class SendManueltVarselbrevTask(val behandlingRepository: BehandlingRepository,
                                val manueltVarselBrevService: ManueltVarselbrevService,
                                val behandlingskontrollService: BehandlingskontrollService) : AsyncTaskStep {

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val maltype = Dokumentmalstype.valueOf(task.metadata.getProperty("maltype"))
        val fritekst = task.metadata.getProperty("fritekst")
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        // sjekk om behandlingen har verge

        if (Dokumentmalstype.VARSEL == maltype) {
            if (behandling.harVerge) {
                manueltVarselBrevService.sendManueltVarselBrev(behandling, fritekst, Brevmottager.VERGE)
            }
            manueltVarselBrevService.sendManueltVarselBrev(behandling, fritekst, Brevmottager.BRUKER)

        } else if (Dokumentmalstype.KORRIGERT_VARSEL == maltype) {
            if (behandling.harVerge) {
                manueltVarselBrevService.sendKorrigertVarselBrev(behandling, fritekst, Brevmottager.VERGE)
            }
            manueltVarselBrevService.sendKorrigertVarselBrev(behandling, fritekst, Brevmottager.BRUKER)
        }

        // utvider fristen bare når tasken ikke kjørte ordentlig ved første omgang
        if (task.opprettetTid.toLocalDate() < LocalDate.now()) {
            val fristTid = LocalDate.now().plus(Constants.brukersSvarfrist).plusDays(1)
            behandlingskontrollService.settBehandlingPåVent(behandling.id,
                                                            Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                                                            fristTid)
        }
    }

    companion object {

        const val TYPE = "brev.sendManueltVarsel"
    }
}
