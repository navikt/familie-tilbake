package no.nav.familie.tilbake.behandling.batch

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.steg.StegService
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class AutomatiskGjenopptaBehandlingService(
    private val behandlingRepository: BehandlingRepository,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val historikkTaskService: HistorikkTaskService,
    private val stegService: StegService,
    private val oppgaveTaskService: OppgaveTaskService
) {

    fun hentAlleBehandlingerKlarForGjenoppta(): List<Behandling> {
        return behandlingRepository.finnAlleBehandlingerKlarForGjenoppta(dagensdato = LocalDate.now())
    }

    @Transactional
    fun gjenopptaBehandling(behandlingId: UUID) {
        val behandlingsstegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingId)
            ?: error("Behandling $behandlingId har ikke aktivt steg")
        val tidsfrist = behandlingsstegstilstand.tidsfrist
            ?: error("Behandling $behandlingId er på vent uten tidsfrist")

        stegService.gjenopptaSteg(behandlingId)
        historikkTaskService.lagHistorikkTask(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT,
            Aktør.VEDTAKSLØSNING
        )

        oppgaveTaskService.oppdaterOppgaveTask(
            behandlingId = behandlingId,
            beskrivelse = "Behandling er tatt av vent automatisk",
            frist = tidsfrist,
            saksbehandler = ContextService.hentSaksbehandler()
        )

        // oppdaterer oppgave hvis saken er fortsatt på vent,
        // f.eks saken var på vent med brukerstilbakemelding og har ikke fått kravgrunnlag
        val aktivStegstilstand = behandlingskontrollService.finnAktivStegstilstand(behandlingId)
        if (aktivStegstilstand?.behandlingsstegsstatus == Behandlingsstegstatus.VENTER) {
            oppgaveTaskService.oppdaterOppgaveTaskMedTriggertid(
                behandlingId = behandlingId,
                beskrivelse = aktivStegstilstand.venteårsak!!.beskrivelse,
                frist = aktivStegstilstand.tidsfrist!!,
                triggerTid = 2L
            )
        }
    }
}
