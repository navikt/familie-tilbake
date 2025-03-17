package no.nav.familie.tilbake.behandling.steg

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandlingskontroll.BehandlingskontrollService
import no.nav.familie.tilbake.behandlingskontroll.Behandlingsstegsinfo
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.dokumentbestilling.vedtak.PeriodeService
import no.nav.familie.tilbake.dokumentbestilling.vedtak.VedtaksbrevsoppsummeringRepository
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.SkalSammenslåPerioder
import no.nav.familie.tilbake.dokumentbestilling.vedtak.domain.Vedtaksbrevsoppsummering
import no.nav.familie.tilbake.foreldelse.ForeldelseService
import no.nav.familie.tilbake.historikkinnslag.Aktør
import no.nav.familie.tilbake.historikkinnslag.HistorikkService
import no.nav.familie.tilbake.historikkinnslag.TilbakekrevingHistorikkinnslagstype
import no.nav.familie.tilbake.kravgrunnlag.event.EndretKravgrunnlagEvent
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.familie.tilbake.oppgave.OppgaveTaskService
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingService
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg.VILKÅRSVURDERING
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus.AUTOUTFØRT
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus.UTFØRT
import org.springframework.context.event.EventListener
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class Vilkårsvurderingssteg(
    private val behandlingskontrollService: BehandlingskontrollService,
    private val vilkårsvurderingService: VilkårsvurderingService,
    private val foreldelseService: ForeldelseService,
    private val historikkService: HistorikkService,
    private val oppgaveTaskService: OppgaveTaskService,
    private val periodeService: PeriodeService,
    private val vedtaksbrevsoppsummeringRepository: VedtaksbrevsoppsummeringRepository,
    private val behandlingRepository: BehandlingRepository,
) : IBehandlingssteg {
    private val log = TracedLogger.getLogger<Vilkårsvurderingssteg>()

    @Transactional
    override fun utførSteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId er på $VILKÅRSVURDERING steg")
        }
        if (harAllePerioderForeldet(behandlingId)) {
            // hvis det finnes noen periode som ble vurdert før i vilkårsvurdering, må slettes
            vilkårsvurderingService.deaktiverEksisterendeVilkårsvurdering(behandlingId)

            lagHistorikkinnslag(behandlingId, Aktør.Vedtaksløsning)

            behandlingskontrollService.oppdaterBehandlingsstegStatus(
                behandlingId,
                Behandlingsstegsinfo(VILKÅRSVURDERING, AUTOUTFØRT),
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
            info("Behandling $behandlingId er på $VILKÅRSVURDERING steg")
        }
        if (harAllePerioderForeldet(behandlingId)) {
            throw Feil(
                message = "Alle perioder er foreldet for $behandlingId,kan ikke behandle vilkårsvurdering",
                frontendFeilmelding = "Alle perioder er foreldet for $behandlingId,kan ikke behandle vilkårsvurdering",
                logContext = logContext,
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }
        vilkårsvurderingService.lagreVilkårsvurdering(behandlingId, behandlingsstegDto as BehandlingsstegVilkårsvurderingDto)
        oppdatereVedtaksbrevsoppsummering(behandlingId)
        oppgaveTaskService.oppdaterAnsvarligSaksbehandlerOppgaveTask(behandlingId, logContext)

        lagHistorikkinnslag(behandlingId, Aktør.Saksbehandler.fraBehandling(behandlingId, behandlingRepository))

        behandlingskontrollService.oppdaterBehandlingsstegStatus(behandlingId, Behandlingsstegsinfo(VILKÅRSVURDERING, UTFØRT), logContext)
        behandlingskontrollService.fortsettBehandling(behandlingId, logContext)
    }

    @Transactional
    override fun utførStegAutomatisk(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId er på $VILKÅRSVURDERING steg og behandler automatisk..")
        }
        if (harAllePerioderForeldet(behandlingId)) {
            utførSteg(behandlingId, logContext)
            return
        }

        vilkårsvurderingService.lagreFastVilkårForAutomatiskSaksbehandling(behandlingId)
        lagHistorikkinnslag(behandlingId, Aktør.Vedtaksløsning)

        behandlingskontrollService.oppdaterBehandlingsstegStatus(
            behandlingId,
            Behandlingsstegsinfo(VILKÅRSVURDERING, UTFØRT),
            logContext,
        )
        behandlingskontrollService.fortsettBehandling(behandlingId, logContext)
    }

    @Transactional
    override fun gjenopptaSteg(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        log.medContext(logContext) {
            info("Behandling $behandlingId gjenopptar på $VILKÅRSVURDERING steg")
        }
        behandlingskontrollService.oppdaterBehandlingsstegStatus(
            behandlingId,
            Behandlingsstegsinfo(
                VILKÅRSVURDERING,
                Behandlingsstegstatus.KLAR,
            ),
            logContext,
        )
    }

    override fun getBehandlingssteg(): Behandlingssteg = VILKÅRSVURDERING

    @EventListener
    fun deaktiverEksisterendeVilkårsvurdering(endretKravgrunnlagEvent: EndretKravgrunnlagEvent) {
        vilkårsvurderingService.deaktiverEksisterendeVilkårsvurdering(endretKravgrunnlagEvent.behandlingId)
    }

    private fun harAllePerioderForeldet(behandlingId: UUID): Boolean =
        foreldelseService
            .hentAktivVurdertForeldelse(behandlingId)
            ?.foreldelsesperioder
            ?.all { it.erForeldet() } ?: false

    private fun lagHistorikkinnslag(
        behandlingId: UUID,
        aktør: Aktør,
    ) {
        historikkService.lagHistorikkinnslag(
            behandlingId,
            TilbakekrevingHistorikkinnslagstype.VILKÅRSVURDERING_VURDERT,
            aktør,
            LocalDateTime.now(),
        )
    }

    private fun oppdatereVedtaksbrevsoppsummering(behandlingId: UUID) {
        val skalSammenslåPerioder = periodeService.erEnsligForsørgerOgPerioderLike(behandlingId)
        val vedtaksbrevsoppsummering = vedtaksbrevsoppsummeringRepository.findByBehandlingId(behandlingId)

        if (vedtaksbrevsoppsummering != null) {
            vedtaksbrevsoppsummeringRepository.update(vedtaksbrevsoppsummering.copy(skalSammenslåPerioder = skalSammenslåPerioder))
        }

        if (skalSammenslåPerioder == SkalSammenslåPerioder.JA && vedtaksbrevsoppsummering == null) {
            vedtaksbrevsoppsummeringRepository.insert(Vedtaksbrevsoppsummering(behandlingId = behandlingId, skalSammenslåPerioder = SkalSammenslåPerioder.JA, oppsummeringFritekst = null))
        }
    }
}
