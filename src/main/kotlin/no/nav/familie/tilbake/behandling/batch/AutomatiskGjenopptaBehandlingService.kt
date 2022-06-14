package no.nav.familie.tilbake.behandling.batch

import no.nav.familie.kontrakter.felles.historikkinnslag.Aktør
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.BehandlingService
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandlingskontroll.domain.Venteårsak
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class AutomatiskGjenopptaBehandlingService(
    private val behandlingRepository: BehandlingRepository,
    private val behandlingService: BehandlingService,
    private val historikkTaskService: HistorikkTaskService,
) {

    fun hentAlleBehandlingerKlarForGjenoppta(): List<Behandling> {
        return behandlingRepository.finnAlleBehandlingerKlarForGjenoppta(
            // behandlinger som venter på VENT_PÅ_BRUKERTILBAKEMELDING og har tidsfrist som er gamle enn defaultVenteTid
            fristDato = LocalDate.now().minusWeeks(Venteårsak.VENT_PÅ_BRUKERTILBAKEMELDING.defaultVenteTidIUker),
            // behandlinger som venter på andre årsak og har tidsfrist som er gamle enn dagens dato
            dagensdato = LocalDate.now()
        )
    }

    @Transactional
    fun gjenopptaBehandling(behandlingId: UUID) {
        behandlingService.oppdaterAnsvarligSaksbehandler(behandlingId)

        historikkTaskService.lagHistorikkTask(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.BEHANDLING_GJENOPPTATT,
            Aktør.VEDTAKSLØSNING
        )

        behandlingService.gjenopptaBehandling(behandlingId)
    }
}
