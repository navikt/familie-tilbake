package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.api.dto.BehandlingsstegDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegForeldelseDto
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingssteg
import no.nav.familie.tilbake.behandlingskontroll.domain.Behandlingsstegstatus
import no.nav.familie.tilbake.foreldelse.ForeldelseService
import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkTaskService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.event.EndretKravgrunnlagEvent
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class Foreldelsessteg(
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val behandlingskontrollService: BehandlingskontrollService,
    private val foreldelseService: ForeldelseService,
    private val historikkTaskService: HistorikkTaskService,
    @Value("\${FORELDELSE_ANTALL_MÅNED:30}")
    private val foreldelseAntallMåned: Long,
    private val oppgaveTaskService: OppgaveTaskService,
) : IBehandlingssteg {
    private val log = TracedLogger.getLogger<Foreldelsessteg>()

    @Transactional
    override fun utførSteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId er på ${Behandlingssteg.FORELDELSE} steg")
        }
        if (!harGrunnlagForeldetPeriode(behandlingId)) {
            lagHistorikkinnslag(behandlingId, Aktør.VEDTAKSLØSNING)

            behandlingskontrollService.oppdaterBehandlingsstegStatus(
                behandlingId,
                Behandlingsstegsinfo(
                    Behandlingssteg.FORELDELSE,
                    Behandlingsstegstatus.AUTOUTFØRT,
                ),
                logContext,
            )
            behandlingskontrollService.fortsettBehandling(behandlingId, logContext)
        }
    }

    @Transactional
    override fun utførSteg(
        behandlingId: UUID,
        behandlingsstegDto: BehandlingsstegDto,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId er på ${Behandlingssteg.FORELDELSE} steg")
        }
        foreldelseService.lagreVurdertForeldelse(behandlingId, (behandlingsstegDto as BehandlingsstegForeldelseDto), logContext)

        oppgaveTaskService.oppdaterAnsvarligSaksbehandlerOppgaveTask(behandlingId, logContext)

        lagHistorikkinnslag(behandlingId, Aktør.SAKSBEHANDLER)

        behandlingskontrollService.oppdaterBehandlingsstegStatus(
            behandlingId,
            Behandlingsstegsinfo(
                Behandlingssteg.FORELDELSE,
                Behandlingsstegstatus.UTFØRT,
            ),
            logContext,
        )
        behandlingskontrollService.fortsettBehandling(behandlingId, logContext)
    }

    @Transactional
    override fun utførStegAutomatisk(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId er på ${Behandlingssteg.FORELDELSE} steg og behandler automatisk..")
        }
        if (harGrunnlagForeldetPeriode(behandlingId)) {
            foreldelseService.lagreFastForeldelseForAutomatiskSaksbehandling(behandlingId, logContext)
            lagHistorikkinnslag(behandlingId, Aktør.VEDTAKSLØSNING)

            behandlingskontrollService.oppdaterBehandlingsstegStatus(
                behandlingId,
                Behandlingsstegsinfo(
                    Behandlingssteg.FORELDELSE,
                    Behandlingsstegstatus.UTFØRT,
                ),
                logContext,
            )
            behandlingskontrollService.fortsettBehandling(behandlingId, logContext)
        } else {
            utførSteg(behandlingId, logContext)
        }
    }

    @Transactional
    override fun gjenopptaSteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId gjenopptar på ${Behandlingssteg.FORELDELSE} steg")
        }
        behandlingskontrollService.oppdaterBehandlingsstegStatus(
            behandlingId,
            Behandlingsstegsinfo(
                Behandlingssteg.FORELDELSE,
                Behandlingsstegstatus.KLAR,
            ),
            logContext,
        )
    }

    private fun harGrunnlagForeldetPeriode(behandlingId: UUID): Boolean {
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        return kravgrunnlag.perioder.any { it.periode.fom.atDay(1) < LocalDate.now().minusMonths(foreldelseAntallMåned) }
    }

    private fun lagHistorikkinnslag(
        behandlingId: UUID,
        aktør: Aktør,
    ) {
        historikkTaskService.lagHistorikkTask(behandlingId, TilbakekrevingHistorikkinnslagstype.FORELDELSE_VURDERT, aktør)
    }

    override fun getBehandlingssteg(): Behandlingssteg = Behandlingssteg.FORELDELSE

    @EventListener
    fun deaktiverEksisterendeVurdertForeldelse(endretKravgrunnlagEvent: EndretKravgrunnlagEvent) {
        foreldelseService.deaktiverEksisterendeVurdertForeldelse(behandlingId = endretKravgrunnlagEvent.behandlingId)
    }
}
