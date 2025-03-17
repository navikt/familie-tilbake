package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.event.EndretKravgrunnlagEvent
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegFaktaDto
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class FaktaFeilutbetalingssteg(
    private val behandlingskontrollService: BehandlingskontrollService,
    private val faktaFeilutbetalingService: FaktaFeilutbetalingService,
    private val historikkService: HistorikkService,
    private val oppgaveTaskService: OppgaveTaskService,
    private val behandlingRepository: BehandlingRepository,
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

        oppgaveTaskService.oppdaterAnsvarligSaksbehandlerOppgaveTask(behandlingId, logContext)

        historikkService.lagHistorikkinnslag(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.FAKTA_VURDERT,
            Aktør.Saksbehandler.fraBehandling(behandlingId, behandlingRepository),
            LocalDateTime.now(),
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

        historikkService.lagHistorikkinnslag(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.FAKTA_VURDERT,
            Aktør.Vedtaksløsning,
            LocalDateTime.now(),
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
