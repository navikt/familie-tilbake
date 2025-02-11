package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.api.dto.BehandlingsstegDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegFaktaDto
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.event.EndretKravgrunnlagEvent
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FaktaFeilutbetalingssteg(
    private val behandlingskontrollService: BehandlingskontrollService,
    private val faktaFeilutbetalingService: FaktaFeilutbetalingService,
    private val historikkTaskService: HistorikkTaskService,
    private val oppgaveTaskService: OppgaveTaskService,
) : IBehandlingssteg {
    private val log = TracedLogger.getLogger<FaktaFeilutbetalingssteg>()

    override fun utførSteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        // Denne metoden gjør ingenting. Det skrives bare for å unngå feilen når ENDR kravgrunnlag mottas
    }

    @Transactional
    override fun utførSteg(
        behandlingId: UUID,
        behandlingsstegDto: BehandlingsstegDto,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId er på ${Behandlingssteg.FAKTA} steg")
        }
        val behandlingsstegFaktaDto: BehandlingsstegFaktaDto = behandlingsstegDto as BehandlingsstegFaktaDto

        faktaFeilutbetalingService.lagreFaktaomfeilutbetaling(behandlingId, behandlingsstegFaktaDto, logContext)

        oppgaveTaskService.oppdaterAnsvarligSaksbehandlerOppgaveTask(behandlingId)

        historikkTaskService.lagHistorikkTask(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.FAKTA_VURDERT,
            Aktør.SAKSBEHANDLER,
        )

        if (faktaFeilutbetalingService.hentAktivFaktaOmFeilutbetaling(behandlingId) != null) {
            flyttBehandlingVidere(behandlingId, logContext)
        }
    }

    @Transactional
    override fun utførStegAutomatisk(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId er på ${Behandlingssteg.FAKTA} steg og behandler automatisk..")
        }
        faktaFeilutbetalingService.lagreFastFaktaForAutomatiskSaksbehandling(behandlingId)

        historikkTaskService.lagHistorikkTask(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.FAKTA_VURDERT,
            Aktør.VEDTAKSLØSNING,
        )

        flyttBehandlingVidere(behandlingId, logContext)
    }

    @Transactional
    override fun gjenopptaSteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId gjenopptar på ${Behandlingssteg.FAKTA} steg")
        }
        behandlingskontrollService.oppdaterBehandlingsstegStatus(
            behandlingId,
            Behandlingsstegsinfo(
                Behandlingssteg.FAKTA,
                Behandlingsstegstatus.KLAR,
            ),
            logContext,
        )
    }

    override fun getBehandlingssteg(): Behandlingssteg = Behandlingssteg.FAKTA

    @EventListener
    fun deaktiverEksisterendeFaktaOmFeilutbetaling(endretKravgrunnlagEvent: EndretKravgrunnlagEvent) {
        faktaFeilutbetalingService.deaktiverEksisterendeFaktaOmFeilutbetaling(behandlingId = endretKravgrunnlagEvent.behandlingId)
    }

    private fun flyttBehandlingVidere(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        behandlingskontrollService.oppdaterBehandlingsstegStatus(
            behandlingId,
            Behandlingsstegsinfo(
                Behandlingssteg.FAKTA,
                Behandlingsstegstatus.UTFØRT,
            ),
            logContext,
        )
        behandlingskontrollService.fortsettBehandling(behandlingId, logContext)
    }
}
