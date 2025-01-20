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
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

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
) : AsyncTaskStep {
    private val logger = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        logger.info("HenleggTilbakekrevingsbehandlingUtenKravgrunnlag prosesserer med id=${task.id} og metadata ${task.metadata}")
        val behandlingId = UUID.fromString(task.payload)
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)

        if (brevSporingService.erVarselSendt(behandlingId)) {
            println("ytelse: ${task.fagsystem()}")
            val beskrivelse =
                "Tilbakekrevingsbehandlingen for stønad ${task.fagsystem()} opprettet ${behandling.opprettetDato} ble opprettet for over 8 uker siden og har ikke mottatt kravgrunnlag. " +
                    "Med mindre det er foretatt en revurdering med tilbakekrevingsbeløp i dag eller de siste dagene for stønaden, så vil det ikke oppstå et kravgrunnlag i dette tilfellet. Tilbakekrevingsbehandlingen kan derfor henlegges manuelt."
            oppgaveTaskService.opprettOppgaveTask(behandling, Oppgavetype.VurderHenvendelse, beskrivelse = beskrivelse)
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
