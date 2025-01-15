package no.nav.familie.tilbake.behandling.batch

import no.nav.familie.kontrakter.felles.oppgave.Oppgavetype
import no.nav.familie.leader.LeaderClient
import no.nav.familie.prosessering.domene.Task
import no.nav.familie.prosessering.internal.TaskService
import no.nav.familie.tilbake.api.dto.HenleggelsesbrevFritekstDto
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.dokumentbestilling.felles.BrevsporingService
import no.nav.familie.tilbake.oppgave.LagOppgaveTask
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class AutomatiskBehandlingRydderBatch(
    private val automatiskBehandlingRydderService: AutomatiskBehandlingRydderService,
    private val behandlingService: BehandlingService,
    private val brevSporingService: BrevsporingService,
    private val oppgaveTaskService: OppgaveTaskService,
    private val taskService: TaskService,
    private val environment: Environment,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    @Scheduled(cron = "\${CRON_AUTOMATISK_GJENOPPTA}")
    fun automatiskFjerningAvGammelBehandlingerUtenKravgrunnlag() {
        if (LeaderClient.isLeader() != true &&
            !environment.activeProfiles.any {
                it.contains("local") || it.contains("integrasjonstest")
            }
        ) {
            return
        }

        logger.info("Starter AutomatiskRyddBehandlingBatch...")
        logger.info("Fjerner alle behandlinger som er eldre enn 8 uker og har ingen kravgrunnlag.")
        val behandlinger = automatiskBehandlingRydderService.hentGammelBehandlingerUtenKravgrunnlag()

        if (behandlinger.isNotEmpty()) {
            val (behandlingerMedBrev, behandlingerUtenBrev) =
                behandlinger.partition {
                    brevSporingService.erVarselSendt(it.id)
                }

            behandlingerMedBrev.forEach { behandling ->
                // Sjekk om en oppgave allerede finnes
                // Opprette en oppgave i fagsystem for å henlegge manuelt og ev. sende et brev.
                // Lurer på om oppgaver til saksbehandlere opprettes slik?
                // Var også litt usikker på Oppgave type her!
                // Hvordan skal forklares at behandlingen kanskje skal henlegges pga manglende kravgrunlagg?
                // Er det riktig måte å sjekke om en lignende task for denne ikke ble opprettet i går? for å unngå flere opprettelse av samme task
                var finnesAlleredeEnTask: Task? = taskService.finnTaskMedPayloadOgType(behandling.id.toString(), LagOppgaveTask.TYPE)

                if (finnesAlleredeEnTask == null ||
                    !finnesAlleredeEnTask.metadata.getProperty("oppgavetype").equals(Oppgavetype.VurderHenvendelse)
                ) {
                    oppgaveTaskService.opprettOppgaveTask(behandling, Oppgavetype.VurderHenvendelse)
                }
            }

            behandlingerUtenBrev.forEach { behandling ->
                behandlingService.henleggBehandling(
                    behandling.id,
                    HenleggelsesbrevFritekstDto(
                        behandlingsresultatstype = Behandlingsresultatstype.HENLAGT_MANGLENDE_KRAVGRUNNLAG,
                        begrunnelse = "",
                    ),
                    true,
                )
            }

            logger.info("Fjernet ${behandlinger.size} behandlinger uten kravgrunnlag som var eldre enn 8 uker.")
        } else {
            logger.info("Ingen gammel behandlinger uten kravgrunnlag.")
        }
        logger.info("Rydding ferdig.")
    }
}
