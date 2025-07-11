package no.nav.familie.tilbake.dokumentbestilling.innhentdokumentasjon

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.Fagsystem
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.familie.tilbake.log.SecureLog.Context.Companion.logContext
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = InnhentDokumentasjonbrevTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Sender innhent dokumentasjonsbrev",
    triggerTidVedFeilISekunder = 60 * 5L,
)
class InnhentDokumentasjonbrevTask(
    private val behandlingRepository: BehandlingRepository,
    private val innhentDokumentasjonBrevService: InnhentDokumentasjonbrevService,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val oppgaveTaskService: OppgaveTaskService,
) : AsyncTaskStep {
    override fun doTask(task: Task) {
        val taskdata: InnhentDokumentasjonbrevTaskdata = objectMapper.readValue(task.payload)
        val behandling = behandlingRepository.findByIdOrThrow(taskdata.behandlingId)
        val fritekst: String = taskdata.fritekst
        val logContext = task.logContext()

        innhentDokumentasjonBrevService.sendInnhentDokumentasjonBrev(behandling, fritekst)

        val fristTid = Constants.saksbehandlersTidsfrist()
        oppgaveTaskService.oppdaterOppgaveTask(
            behandlingId = behandling.id,
            beskrivelse = "Frist er oppdatert. Saksbehandler ${behandling
                .ansvarligSaksbehandler} har bedt om mer informasjon av bruker",
            frist = fristTid,
            saksbehandler = behandling.ansvarligSaksbehandler,
            logContext = logContext,
        )
        // Oppdaterer fristen dersom tasken har tidligere feilet. Behandling ble satt på vent i DokumentBehandlingService.
        if (task.opprettetTid.toLocalDate() < LocalDate.now()) {
            behandlingskontrollService.settBehandlingPåVent(
                behandling.id,
                Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING,
                fristTid,
                logContext,
            )
        }
    }

    companion object {
        fun opprettTask(
            behandlingId: UUID,
            fagsystem: Fagsystem,
            fritekst: String,
        ): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(InnhentDokumentasjonbrevTaskdata(behandlingId, fritekst)),
                properties = Properties().apply { setProperty(PropertyName.FAGSYSTEM, fagsystem.name) },
            ).medTriggerTid(LocalDateTime.now().plusSeconds(15))

        const val TYPE = "brev.sendInnhentDokumentasjon"
    }
}

data class InnhentDokumentasjonbrevTaskdata(
    val behandlingId: UUID,
    val fritekst: String,
)
