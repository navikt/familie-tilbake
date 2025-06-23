package no.nav.familie.tilbake.foreldelse

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandlingskontroll.BehandlingsstegstilstandRepository
import no.nav.familie.tilbake.beregning.validatePerioder
import no.nav.familie.tilbake.common.expectSingleOrNull
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.faktaomfeilutbetaling.LogiskPeriodeUtil
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagUtil
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import no.nav.tilbakekreving.api.v1.dto.BehandlingsstegForeldelseDto
import no.nav.tilbakekreving.api.v1.dto.ForeldelsesperiodeDto
import no.nav.tilbakekreving.api.v1.dto.VurdertForeldelseDto
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ForeldelseService(
    private val foreldelseRepository: VurdertForeldelseRepository,
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val behandlingRepository: BehandlingRepository,
    private val behandlingsstegstilstandRepository: BehandlingsstegstilstandRepository,
    private val logService: LogService,
) {
    fun hentVurdertForeldelse(behandlingId: UUID): VurdertForeldelseDto {
        val logContext = logService.contextFraBehandling(behandlingId)
        val vurdertForeldelse: VurdertForeldelse? = foreldelseRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
            .expectSingleOrNull(logContext) { "id=${it.id}, ${it.sporbar.opprettetTid}" }
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        // Faktaperioder kan ikke deles. Så logisk periode er samme som faktaperiode
        val feilutbetaltePerioder =
            LogiskPeriodeUtil
                .utledLogiskPeriode(KravgrunnlagUtil.finnFeilutbetalingPrPeriode(kravgrunnlag))

        return ForeldelseMapper.tilRespons(feilutbetaltePerioder, kravgrunnlag, vurdertForeldelse)
    }

    fun hentAktivVurdertForeldelse(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ): VurdertForeldelse? = foreldelseRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        .expectSingleOrNull(logContext) { "id=${it.id}, ${it.sporbar.opprettetTid}" }

    fun hentAlleForeldelser(behandlingId: UUID): List<VurdertForeldelse> = foreldelseRepository.findByBehandlingId(behandlingId)

    fun erPeriodeForeldet(
        behandlingId: UUID,
        periode: Månedsperiode,
        logContext: SecureLog.Context,
    ): Boolean =
        hentAktivVurdertForeldelse(behandlingId, logContext)
            ?.foreldelsesperioder
            ?.any { periode == it.periode && it.erForeldet() }
            ?: false

    @Transactional
    fun lagreVurdertForeldelse(
        behandlingId: UUID,
        behandlingsstegForeldelseDto: BehandlingsstegForeldelseDto,
        logContext: SecureLog.Context,
    ) {
        // Alle familieytelsene er månedsytelser. Så periode som skal lagres bør være innenfor en måned
        validatePerioder(behandlingsstegForeldelseDto.foreldetPerioder.map { it.periode }, logContext)
        val vurdertForeldelse = ForeldelseMapper.tilDomene(behandlingId, behandlingsstegForeldelseDto.foreldetPerioder)

        nullstillVilkårsvurderingForEndringerIForeldelsesperiode(behandlingId, vurdertForeldelse, logContext)
        deaktiverEksisterendeVurdertForeldelse(behandlingId, logContext)
        foreldelseRepository.insert(vurdertForeldelse)
    }

    @Transactional
    fun lagreFastForeldelseForAutomatiskSaksbehandling(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val foreldetPerioder =
            hentVurdertForeldelse(behandlingId).foreldetPerioder.map {
                ForeldelsesperiodeDto(
                    periode = it.periode,
                    begrunnelse =
                        Constants.hentAutomatiskForeldelsesbegrunnelse(
                            behandling.saksbehandlingstype,
                            logContext = logContext,
                        ),
                    foreldelsesvurderingstype = Foreldelsesvurderingstype.IKKE_FORELDET,
                )
            }
        foreldelseRepository.insert(ForeldelseMapper.tilDomene(behandlingId, foreldetPerioder))
    }

    @Transactional
    fun deaktiverEksisterendeVurdertForeldelse(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ) {
        foreldelseRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
            .expectSingleOrNull(logContext) { "id=${it.id}, ${it.sporbar.opprettetTid}" }
            ?.copy(aktiv = false)?.let {
                foreldelseRepository.update(it)
            }
    }

    @Transactional
    fun nullstillVilkårsvurderingForEndringerIForeldelsesperiode(
        behandlingId: UUID,
        vurdertForeldelse: VurdertForeldelse,
        logContext: SecureLog.Context,
    ) {
        val eksisterendeVurdertForeldelse = foreldelseRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
            .expectSingleOrNull(logContext) { "id=${it.id}, ${it.sporbar.opprettetTid}" } ?: return
        val eksisterendeVurdertForeldelsesperioder = eksisterendeVurdertForeldelse.foreldelsesperioder.map { it.periode }.toSet()
        val nyVurdertForeldelsesperioder = vurdertForeldelse.foreldelsesperioder.map { it.periode }.toSet()
        val endringerIPeriode = eksisterendeVurdertForeldelsesperioder.minus(nyVurdertForeldelsesperioder)

        if (endringerIPeriode.isEmpty()) return
        // Hvis foreldelsesperioder har endret, må saksbehandler behandle vilkårsvurdering på nytt
        vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
            .expectSingleOrNull(logContext) { "id=${it.id}, ${it.sporbar.opprettetTid}" }
            ?.copy(aktiv = false)?.let { vilkårsvurdering ->
                vilkårsvurderingRepository.update(vilkårsvurdering)
            }
    }

    fun sjekkOmForeldelsePerioderErLike(
        behandlingId: UUID,
        logContext: SecureLog.Context,
    ): Boolean {
        val behandlingssteg = behandlingsstegstilstandRepository.findByBehandlingIdAndBehandlingssteg(behandlingId, no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg.FORELDELSE)
        if (behandlingssteg?.behandlingsstegsstatus == no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus.AUTOUTFØRT) {
            return true
        }
        val vurdertForeldelse: VurdertForeldelse? = foreldelseRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
            .expectSingleOrNull(logContext) { "id=${it.id}, ${it.sporbar.opprettetTid}" }
        val førstePeriode = vurdertForeldelse?.foreldelsesperioder?.firstOrNull() ?: return false

        return vurdertForeldelse.foreldelsesperioder.all {
            it.erLik(førstePeriode)
        }
    }
}
