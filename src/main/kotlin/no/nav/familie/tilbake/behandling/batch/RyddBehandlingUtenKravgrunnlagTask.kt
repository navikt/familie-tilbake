package no.nav.familie.tilbake.behandling.batch

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.objectMapper
import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.api.dto.HenleggelsesbrevFritekstDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.common.fagsystem
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingService
import no.nav.familie.tilbake.dokumentbestilling.henleggelse.SendBrevTaskdata
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import no.nav.familie.tilbake.oppgave.OppgavePrioritetService
import no.nav.familie.tilbake.oppgave.OppgaveService
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = RyddBehandlingUtenKravgrunnlagTask.TYPE,
    beskrivelse = "Henlegger tilbakekrevingsbehandling uten kravgrunnlag",
    maxAntallFeil = 3,
    triggerTidVedFeilISekunder = 60 * 5L,
)
class RyddBehandlingUtenKravgrunnlagTask(
    private val behandlingService: BehandlingService,
    private val behandlingRepository: BehandlingRepository,
    private val brevSporingService: BrevsporingService,
    private val oppgaveTaskService: OppgaveTaskService,
    private val logService: LogService,
    private val oppgaveService: OppgaveService,
    private val oppgavePrioritetService: OppgavePrioritetService,
) : AsyncTaskStep {
    private val log = TracedLogger.getLogger<RyddBehandlingUtenKravgrunnlagTask>()

    override fun doTask(task: Task) {
        val behandlingId = UUID.fromString(task.payload)
        val logContext = logService.contextFraBehandling(behandlingId)
        log.medContext(logContext) {
            info("HenleggTilbakekrevingsbehandlingUtenKravgrunnlag prosesserer med id={} og metadata {}", task.id, task.metadata.toString())
        }
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)

        if (brevSporingService.erVarselSendt(behandlingId)) {
            val prioritet = oppgavePrioritetService.utledOppgaveprioritet(behandling.id)
            val fristForFerdigstillelse = LocalDate.now().plusDays(7)

            val beskrivelse =
                "Tilbakekrevingsbehandlingen for stønad ${task.fagsystem()} opprettet ${behandling.opprettetDato} ble opprettet for over 8 uker siden og har ikke mottatt kravgrunnlag. " +
                    "Med mindre det er foretatt en revurdering med tilbakekrevingsbeløp i dag eller de siste dagene for stønaden, så vil det ikke oppstå et kravgrunnlag i dette tilfellet. Tilbakekrevingsbehandlingen kan derfor henlegges manuelt."
            oppgaveService.opprettOppgaveUtenSaksIdOgBehandlesAvApplikasjon(
                behandlingId,
                Oppgavetype.VurderHenvendelse,
                behandling.behandlendeEnhet,
                beskrivelse,
                fristForFerdigstillelse,
                behandling.ansvarligSaksbehandler,
                prioritet,
            )
        } else {
            behandlingService.henleggBehandling(
                behandling.id,
                HenleggelsesbrevFritekstDto(
                    behandlingsresultatstype = Behandlingsresultatstype.HENLAGT_MANGLENDE_KRAVGRUNNLAG,
                    begrunnelse = "",
                ),
            )
        }
    }

    companion object {
        fun opprettTask(
            behandlingId: UUID,
            fagsystem: Fagsystem,
            fritekst: String?,
        ): Task =
            Task(
                type = TYPE,
                payload = objectMapper.writeValueAsString(SendBrevTaskdata(behandlingId, fritekst)),
                properties = Properties().apply { setProperty(PropertyName.FAGSYSTEM, fagsystem.name) },
            ).medTriggerTid(LocalDateTime.now().plusSeconds(15))

        const val TYPE = "rydd.behandling.utenkravgrunnlag"
    }
}
