package no.nav.familie.tilbake.oppgave

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.prosessering.AsyncTaskStep
import no.nav.familie.prosessering.TaskStepBeskrivelse
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.config.PropertyName
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

@Service
@TaskStepBeskrivelse(
    taskStepType = LagOppgaveTask.TYPE,
    maxAntallFeil = 3,
    beskrivelse = "Lager oppgave for nye behandlinger",
    triggerTidVedFeilISekunder = 300L
)
class LagOppgaveTask(
    private val oppgaveService: OppgaveService,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val integrasjonerClient: IntegrasjonerClient
) : AsyncTaskStep {

    private val log = LoggerFactory.getLogger(this::class.java)

    override fun doTask(task: Task) {
        log.info("LagOppgaveTask prosesserer med id=${task.id} og metadata ${task.metadata}")
        val oppgavetype = Oppgavetype.valueOf(task.metadata.getProperty("oppgavetype"))
        val saksbehandler = task.metadata.getProperty("saksbehandler")
        val enhet = task.metadata.getProperty(PropertyName.ENHET) ?: "" // elvis-operator for bakoverkompatibilitet
        val behandlingId = UUID.fromString(task.payload)

        val behandlingsstegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingId)

        val sendtTilBeslutningAv = if (behandlingsstegstilstand?.behandlingssteg == Behandlingssteg.FATTE_VEDTAK) {
            val opprettetAv: String? = task.metadata.getProperty("opprettetAv")
            val saksbehandlerNavn = opprettetAv?.let {
                integrasjonerClient.hentSaksbehandler(it).let {
                    "${it.fornavn} ${it.etternavn}"
                }
            }
            saksbehandlerNavn?.let { "Sendt til godkjenning av $it. " }
        } else {
            null
        }

        val fristeUker = behandlingsstegstilstand?.venteårsak?.defaultVenteTidIUker ?: 0
        val venteårsak = behandlingsstegstilstand?.venteårsak?.beskrivelse
        val beskrivelse = if (sendtTilBeslutningAv != null) {
            sendtTilBeslutningAv + (venteårsak ?: "")
        } else {
            venteårsak
        }

        oppgaveService.opprettOppgave(
            UUID.fromString(task.payload),
            oppgavetype,
            enhet,
            beskrivelse,
            LocalDate.now().plusWeeks(fristeUker),
            saksbehandler
        )
    }

    companion object {

        const val TYPE = "lagOppgave"
    }
}
