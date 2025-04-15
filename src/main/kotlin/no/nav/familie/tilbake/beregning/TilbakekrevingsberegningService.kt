package no.nav.familie.tilbake.beregning

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingMapper
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.foreldelse.VurdertForeldelseRepository
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesperiode
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Fagområdekode
import no.nav.familie.tilbake.log.LogService
import no.nav.familie.tilbake.vilkårsvurdering.VilkårsvurderingRepository
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurdering
import no.nav.tilbakekreving.api.v1.dto.BeregnetPeriodeDto
import no.nav.tilbakekreving.api.v1.dto.BeregnetPerioderDto
import no.nav.tilbakekreving.api.v1.dto.BeregningsresultatDto
import no.nav.tilbakekreving.api.v1.dto.BeregningsresultatsperiodeDto
import no.nav.tilbakekreving.beregning.Beregning
import no.nav.tilbakekreving.beregning.KravgrunnlagsberegningUtil
import no.nav.tilbakekreving.beregning.modell.Beregningsresultat
import no.nav.tilbakekreving.kontrakter.behandling.Saksbehandlingstype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class TilbakekrevingsberegningService(
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val vurdertForeldelseRepository: VurdertForeldelseRepository,
    private val vilkårsvurderingRepository: VilkårsvurderingRepository,
    private val behandlingRepository: BehandlingRepository,
    private val faktaFeilutbetalingService: FaktaFeilutbetalingService,
    private val logService: LogService,
) {
    fun hentBeregningsresultat(behandlingId: UUID): BeregningsresultatDto {
        val beregningsresultat = beregn(behandlingId)
        val beregningsresultatsperioder = beregningsresultat.beregningsresultatsperioder.map {
            BeregningsresultatsperiodeDto(
                periode = it.periode,
                vurdering = it.vurdering,
                feilutbetaltBeløp = it.feilutbetaltBeløp,
                andelAvBeløp = it.andelAvBeløp,
                renteprosent = it.renteprosent,
                tilbakekrevingsbeløp = it.tilbakekrevingsbeløp,
                tilbakekrevesBeløpEtterSkatt = it.tilbakekrevingsbeløpEtterSkatt,
            )
        }
        val vurderingAvBrukersUttalelse = faktaFeilutbetalingService.hentAktivFaktaOmFeilutbetaling(behandlingId)?.vurderingAvBrukersUttalelse

        return BeregningsresultatDto(
            beregningsresultatsperioder = beregningsresultatsperioder,
            vedtaksresultat = beregningsresultat.vedtaksresultat,
            vurderingAvBrukersUttalelse = FaktaFeilutbetalingMapper.tilDto(vurderingAvBrukersUttalelse),
        )
    }

    fun beregn(behandlingId: UUID): Beregningsresultat {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        val kravgrunnlagAdapter = Kravgrunnlag431Adapter(kravgrunnlag)
        val vilkårsvurderingAdapter = GammelVilkårsvurderingAdapter(hentVilkårsvurdering(behandlingId))
        val foreldetPerioder = hentVurdertForeldelse(behandlingId)?.let { vurdering ->
            vurdering.foreldelsesperioder
                .filter(Foreldelsesperiode::erForeldet)
                .map { it.periode.toDatoperiode() }
        } ?: emptyList()

        return Beregning(
            beregnRenter = skalBeregneRenter(kravgrunnlag.fagområdekode),
            tilbakekrevLavtBeløp = behandling.saksbehandlingstype == Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP,
            kravgrunnlag = kravgrunnlagAdapter,
            vilkårsvurdering = vilkårsvurderingAdapter,
            foreldetPerioder = foreldetPerioder,
        ).beregn()
    }

    fun beregnBeløp(
        behandlingId: UUID,
        perioder: List<Datoperiode>,
    ): BeregnetPerioderDto {
        val logContext = logService.contextFraBehandling(behandlingId)
        // Alle familieytelsene er månedsytelser. Så periode som skal lagres bør være innenfor en måned.
        validatePerioder(perioder, logContext)
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)

        return BeregnetPerioderDto(
            beregnetPerioder = perioder.map {
                BeregnetPeriodeDto(
                    periode = it,
                    feilutbetaltBeløp = KravgrunnlagsberegningUtil.beregnFeilutbetaltBeløp(Kravgrunnlag431Adapter(kravgrunnlag), it),
                )
            },
        )
    }

    private fun skalBeregneRenter(fagområdekode: Fagområdekode): Boolean =
        when (fagområdekode) {
            Fagområdekode.BA, Fagområdekode.KS -> false
            else -> true
        }

    private fun hentVilkårsvurdering(behandlingId: UUID): Vilkårsvurdering? = vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)

    private fun hentVurdertForeldelse(behandlingId: UUID): VurdertForeldelse? = vurdertForeldelseRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
}
