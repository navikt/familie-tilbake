package no.nav.familie.tilbake.vilkårsvurdering

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.expectSingleOrNull
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants.hentAutomatiskVilkårsvurderingAktsomhetBegrunnelse
import no.nav.familie.tilbake.config.Constants.hentAutomatiskVilkårsvurderingBegrunnelse
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.foreldelse.ForeldelseService
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingAktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.VilkårsvurderingGodTro
import no.nav.tilbakekreving.api.v1.dto.AktsomhetDto
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.tilbakekreving.api.v1.dto.VilkårsvurderingsperiodeDto
import no.nav.tilbakekreving.api.v1.dto.VurdertVilkårsvurderingDto
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Aktsomhet
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.Vilkårsvurderingsresultat
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class VilkårsvurderingService(
    val vilkårsvurderingRepository: VilkårsvurderingRepository,
    val kravgrunnlagRepository: KravgrunnlagRepository,
    val fagsakRepository: FagsakRepository,
    val behandlingRepository: BehandlingRepository,
    val foreldelseService: ForeldelseService,
    val faktaFeilutbetalingService: FaktaFeilutbetalingService,
    private val logService: LogService,
) {
    fun hentVilkårsvurdering(
        behandlingId: UUID,
    ): VurdertVilkårsvurderingDto {
        val logContext = logService.contextFraBehandling(behandlingId)
        val faktaOmFeilutbetaling =
            faktaFeilutbetalingService.hentAktivFaktaOmFeilutbetaling(behandlingId)
                ?: throw Feil(
                    message =
                        "Fakta om feilutbetaling finnes ikke for behandling=$behandlingId, " +
                            "kan ikke hente vilkårsvurdering",
                    logContext = logContext,
                )
        val kravgrunnlag431 = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
            .expectSingleOrNull(logContext) { "id=${it.id}, ${it.sporbar.opprettetTid}" }
        val vurdertForeldelse = foreldelseService.hentAktivVurdertForeldelse(behandlingId, logContext)
        return mapTilVilkårsvurderingDto(vurdertForeldelse, faktaOmFeilutbetaling, vilkårsvurdering, kravgrunnlag431)
    }

    fun hentInaktivVilkårsvurdering(behandlingId: UUID): List<VurdertVilkårsvurderingDto> {
        val alleFakta = faktaFeilutbetalingService.hentAlleFaktaOmFeilutbetaling(behandlingId).sortedBy { it.sporbar.opprettetTid }
        val alleKravgrunnlag = kravgrunnlagRepository.findByBehandlingId(behandlingId).sortedBy { it.sporbar.opprettetTid }
        val alleVilkårsvurderinger = vilkårsvurderingRepository.findByBehandlingId(behandlingId).sortedBy { it.sporbar.opprettetTid }.filter { !it.aktiv }
        val alleForeldelser = foreldelseService.hentAlleForeldelser(behandlingId).sortedBy { it.sporbar.opprettetTid }
        return alleVilkårsvurderinger.map { vilkårsvurdering ->
            val faktaOmFeilutbetaling: FaktaFeilutbetaling = alleFakta.last { it.sporbar.opprettetTid < vilkårsvurdering.sporbar.opprettetTid }
            val vurdertForeldelse = alleForeldelser.lastOrNull { it.sporbar.opprettetTid < vilkårsvurdering.sporbar.opprettetTid }
            val kravgrunnlag431 = alleKravgrunnlag.last { it.sporbar.opprettetTid < vilkårsvurdering.sporbar.opprettetTid }

            mapTilVilkårsvurderingDto(vurdertForeldelse, faktaOmFeilutbetaling, vilkårsvurdering, kravgrunnlag431)
        }
    }

    private fun mapTilVilkårsvurderingDto(
        vurdertForeldelse: VurdertForeldelse?,
        faktaOmFeilutbetaling: FaktaFeilutbetaling,
        vilkårsvurdering: Vilkårsvurdering?,
        kravgrunnlag431: Kravgrunnlag431,
    ): VurdertVilkårsvurderingDto {
        val perioder = mutableListOf<Månedsperiode>()
        val foreldetPerioderMedBegrunnelse = mutableMapOf<Månedsperiode, String>()
        val sortertVilkårsvurdering = vilkårsvurdering?.perioder?.sortedBy { it.periode.fom } ?: emptyList()

        if (vurdertForeldelse == null) {
            // Gjelder ved automatisk behandling under 4x rettsgebyr
            faktaOmFeilutbetaling.perioder.sortedBy { it.periode.fom }.forEach { faktaPeriode ->
                var gjenværendeFaktaPeriode = faktaPeriode.periode
                sortertVilkårsvurdering.forEach { vurdertPeriode ->
                    if (gjenværendeFaktaPeriode.inneholder(vurdertPeriode.periode)) {
                        val periodeFørVurdert = gjenværendeFaktaPeriode.før(vurdertPeriode.periode.fom)
                        val periodeEtterVurdert = gjenværendeFaktaPeriode.etter(vurdertPeriode.periode.tom)

                        periodeFørVurdert?.let { perioder.add(it) }
                        perioder.add(vurdertPeriode.periode)

                        gjenværendeFaktaPeriode = periodeEtterVurdert ?: return@forEach
                    }
                }

                gjenværendeFaktaPeriode.let { perioder.add(it) }
            }
        } else {
            vurdertForeldelse.foreldelsesperioder.sortedBy { it.periode.fom }.forEach { foreldelsesperiode ->
                // Foreldede perioder med begrunnelse i foreldetPerioderMedBegrunnelse
                if (foreldelsesperiode.erForeldet()) {
                    foreldetPerioderMedBegrunnelse[foreldelsesperiode.periode] = foreldelsesperiode.begrunnelse
                }

                var gjenværendeForeldetPeriode = foreldelsesperiode.periode

                sortertVilkårsvurdering.forEach { vurdertPeriode ->
                    if (gjenværendeForeldetPeriode.inneholder(vurdertPeriode.periode)) {
                        val periodeFørVurdert = gjenværendeForeldetPeriode.før(vurdertPeriode.periode.fom)
                        val periodeEtterVurdert = gjenværendeForeldetPeriode.etter(vurdertPeriode.periode.tom)

                        periodeFørVurdert?.let { perioder.add(it) }
                        perioder.add(vurdertPeriode.periode)

                        gjenværendeForeldetPeriode = periodeEtterVurdert ?: return@forEach
                    }
                }

                gjenværendeForeldetPeriode.let { perioder.add(it) }
            }
        }

        return VilkårsvurderingMapper.tilRespons(
            vilkårsvurdering = vilkårsvurdering,
            perioder = perioder.toList(),
            foreldetPerioderMedBegrunnelse = foreldetPerioderMedBegrunnelse.toMap(),
            faktaFeilutbetaling = faktaOmFeilutbetaling,
            kravgrunnlag431 = kravgrunnlag431,
        )
    }

    @Transactional
    fun lagreVilkårsvurdering(
        behandlingId: UUID,
        behandlingsstegVilkårsvurderingDto: BehandlingsstegVilkårsvurderingDto,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val logContext = SecureLog.Context.medBehandling(fagsak.eksternFagsakId, behandling.id.toString())
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        // Valider request
        VilkårsvurderingValidator.validerVilkårsvurdering(
            vilkårsvurderingDto = behandlingsstegVilkårsvurderingDto,
            kravgrunnlag431 = kravgrunnlag,
            logContext = logContext,
        )
        // filter bort perioder som er foreldet
        val ikkeForeldetPerioder =
            behandlingsstegVilkårsvurderingDto.vilkårsvurderingsperioder
                .filter { !foreldelseService.erPeriodeForeldet(behandlingId, Månedsperiode(it.periode.fom, it.periode.tom), logContext) }
        deaktiverEksisterendeVilkårsvurdering(behandlingId, logContext)
        vilkårsvurderingRepository.insert(
            VilkårsvurderingMapper.tilDomene(
                behandlingId = behandlingId,
                vilkårsvurderingsperioder = ikkeForeldetPerioder,
                fagsystem = fagsak.fagsystem,
            ),
        )
    }

    @Transactional
    fun lagreFastVilkårForAutomatiskSaksbehandling(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsak = fagsakRepository.findByIdOrThrow(behandling.fagsakId)
        val logContext = SecureLog.Context.medBehandling(fagsak.eksternFagsakId, behandling.id.toString())

        val perioder = hentVilkårsvurdering(behandlingId).perioder
        val vurdertePerioder =
            perioder.filter { !it.foreldet }.map {
                VilkårsvurderingsperiodeDto(
                    periode = it.periode,
                    vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                    begrunnelse = hentAutomatiskVilkårsvurderingBegrunnelse(behandling.saksbehandlingstype, logContext),
                    aktsomhetDto =
                        AktsomhetDto(
                            aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                            tilbakekrevSmåbeløp = false,
                            begrunnelse = hentAutomatiskVilkårsvurderingAktsomhetBegrunnelse(behandling.saksbehandlingstype, logContext),
                        ),
                )
            }
        vilkårsvurderingRepository.insert(
            VilkårsvurderingMapper.tilDomene(
                behandlingId = behandlingId,
                vilkårsvurderingsperioder = vurdertePerioder,
                fagsystem = fagsak.fagsystem,
            ),
        )
    }

    @Transactional
    fun deaktiverEksisterendeVilkårsvurdering(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
            .expectSingleOrNull(logContext) { "id=${it.id}, ${it.sporbar.opprettetTid}" }
            ?.copy(aktiv = false)?.let {
                vilkårsvurderingRepository.update(it)
            }
    }

    fun sjekkOmVilkårsvurderingPerioderErLike(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ): Boolean {
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
            .expectSingleOrNull(logContext) { "id=${it.id}, ${it.sporbar.opprettetTid}" }
        val førstePeriode = vilkårsvurdering?.perioder?.first()
        return if (førstePeriode != null) {
            vilkårsvurdering.perioder.all {
                erVilkårsvurderingAktsomhetNullEllerLik(it.aktsomhet, førstePeriode.aktsomhet) &&
                    erVilkårsvurderingGodTroNullEllerLik(it.godTro, førstePeriode.godTro) &&
                    it.vilkårsvurderingsresultat == førstePeriode.vilkårsvurderingsresultat &&
                    it.begrunnelse == førstePeriode.begrunnelse
            }
        } else {
            false
        }
    }

    private fun erVilkårsvurderingAktsomhetNullEllerLik(
        gjeldendeVilkårsvurderingAktsomhet: VilkårsvurderingAktsomhet?,
        førsteVilkårsvurderingAktsomhet: VilkårsvurderingAktsomhet?,
    ) = (gjeldendeVilkårsvurderingAktsomhet == null && førsteVilkårsvurderingAktsomhet == null) || gjeldendeVilkårsvurderingAktsomhet?.erLik(førsteVilkårsvurderingAktsomhet) == true

    private fun erVilkårsvurderingGodTroNullEllerLik(
        gjeldendeVilkårsvurderingGodTro: VilkårsvurderingGodTro?,
        førsteVilkårsvurderingGodTro: VilkårsvurderingGodTro?,
    ) = (gjeldendeVilkårsvurderingGodTro == null && førsteVilkårsvurderingGodTro == null) || gjeldendeVilkårsvurderingGodTro?.erLik(førsteVilkårsvurderingGodTro) == true

    private fun erPeriodeAlleredeVurdert(
        vilkårsvurdering: Vilkårsvurdering?,
        periode: Månedsperiode,
    ): Boolean = vilkårsvurdering?.perioder?.any { periode.inneholder(it.periode) } == true
}
