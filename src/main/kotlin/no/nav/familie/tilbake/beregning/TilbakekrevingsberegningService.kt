package no.nav.familie.tilbake.beregning

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
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
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsperiode
import no.nav.tilbakekreving.api.v1.dto.BeregnetPeriodeDto
import no.nav.tilbakekreving.api.v1.dto.BeregnetPerioderDto
import no.nav.tilbakekreving.api.v1.dto.BeregningsresultatDto
import no.nav.tilbakekreving.api.v1.dto.BeregningsresultatsperiodeDto
import no.nav.tilbakekreving.beregning.KravgrunnlagsberegningUtil
import no.nav.tilbakekreving.beregning.VilkårsvurderingBeregning
import no.nav.tilbakekreving.beregning.modell.Beregningsresultat
import no.nav.tilbakekreving.beregning.modell.Beregningsresultatsperiode
import no.nav.tilbakekreving.beregning.modell.FordeltKravgrunnlagsbeløp
import no.nav.tilbakekreving.kontrakter.behandling.Saksbehandlingstype
import no.nav.tilbakekreving.kontrakter.beregning.Vedtaksresultat
import no.nav.tilbakekreving.kontrakter.foreldelse.Foreldelsesvurderingstype
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import no.nav.tilbakekreving.kontrakter.periode.Månedsperiode
import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.AnnenVurdering
import org.springframework.stereotype.Service
import java.math.BigDecimal
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
        val beregningsresultatsperioder =
            beregningsresultat.beregningsresultatsperioder.map {
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
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        val kravgrunnlagAdapter = Kravgrunnlag431Adapter(kravgrunnlag)
        val vurdertForeldelse = hentVurdertForeldelse(behandlingId)
        val vilkårsvurdering = hentVilkårsvurdering(behandlingId)
        val vurderingsperioder: List<Månedsperiode> = finnPerioder(vurdertForeldelse, vilkårsvurdering)
        val perioderMedBeløp: Map<Datoperiode, FordeltKravgrunnlagsbeløp> =
            KravgrunnlagsberegningUtil.fordelKravgrunnlagBeløpPåPerioder(kravgrunnlagAdapter, vurderingsperioder.map { it.toDatoperiode() })
        val vilkårsvurderingBeregning =
            if (vilkårsvurdering == null) {
                null
            } else {
                VilkårsvurderingBeregning(
                    kravgrunnlagAdapter,
                    GammelVilkårsvurderingAdapter(vilkårsvurdering),
                    perioderMedBeløp,
                    skalBeregneRenter(kravgrunnlag.fagområdekode),
                )
            }
        val beregningsresultatperioder = beregn(vurdertForeldelse, perioderMedBeløp.mapKeys { it.key.toMånedsperiode() }, vilkårsvurderingBeregning)
        val totalTilbakekrevingsbeløp = beregningsresultatperioder.sumOf { it.tilbakekrevingsbeløp }
        val totalFeilutbetaltBeløp = beregningsresultatperioder.sumOf { it.feilutbetaltBeløp }
        return Beregningsresultat(
            vedtaksresultat =
                bestemVedtakResultat(
                    behandlingId,
                    totalTilbakekrevingsbeløp,
                    totalFeilutbetaltBeløp,
                ),
            beregningsresultatsperioder = (beregningsresultatperioder),
        )
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
            beregnetPerioder =
                perioder.map {
                    val feilutbetaltBeløp =
                        KravgrunnlagsberegningUtil.beregnFeilutbetaltBeløp(Kravgrunnlag431Adapter(kravgrunnlag), it)
                    BeregnetPeriodeDto(
                        periode = it,
                        feilutbetaltBeløp = feilutbetaltBeløp,
                    )
                },
        )
    }

    private fun hentVilkårsvurdering(behandlingId: UUID): Vilkårsvurdering? = vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)

    private fun hentVurdertForeldelse(behandlingId: UUID): VurdertForeldelse? = vurdertForeldelseRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)

    private fun finnPerioder(
        vurdertForeldelse: VurdertForeldelse?,
        vilkårsvurdering: Vilkårsvurdering?,
    ): List<Månedsperiode> = finnForeldedePerioder(vurdertForeldelse) + finnIkkeForeldedePerioder(vilkårsvurdering)

    private fun beregn(
        vurdertForeldelse: VurdertForeldelse?,
        perioderMedBeløp: Map<Månedsperiode, FordeltKravgrunnlagsbeløp>,
        vilkårsvurderingBeregning: VilkårsvurderingBeregning?,
    ): List<Beregningsresultatsperiode> =
        (
            beregnForForeldedePerioder(vurdertForeldelse, perioderMedBeløp) +
                (vilkårsvurderingBeregning?.beregnForIkkeForeldedePerioder() ?: emptyList())
        ).sortedBy { it.periode.fom }

    private fun finnIkkeForeldedePerioder(vilkårsvurdering: Vilkårsvurdering?): List<Månedsperiode> =
        vilkårsvurdering?.perioder?.map(Vilkårsvurderingsperiode::periode)
            ?: emptyList()

    private fun finnForeldedePerioder(vurdertForeldelse: VurdertForeldelse?): List<Månedsperiode> =
        vurdertForeldelse
            ?.foreldelsesperioder
            ?.filter(Foreldelsesperiode::erForeldet)
            ?.map(Foreldelsesperiode::periode)
            ?: emptyList()

    private fun beregnForForeldedePerioder(
        vurdertForeldelse: VurdertForeldelse?,
        kravbeløpPerPeriode: Map<Månedsperiode, FordeltKravgrunnlagsbeløp>,
    ): Collection<Beregningsresultatsperiode> =
        vurdertForeldelse
            ?.foreldelsesperioder
            ?.filter { Foreldelsesvurderingstype.FORELDET == it.foreldelsesvurderingstype }
            ?.map { beregnForeldetPeriode(kravbeløpPerPeriode, it) }
            ?: emptyList()

    private fun beregnForeldetPeriode(
        beløpPerPeriode: Map<Månedsperiode, FordeltKravgrunnlagsbeløp>,
        foreldelsePeriode: Foreldelsesperiode,
    ): Beregningsresultatsperiode {
        val periode: Månedsperiode = foreldelsePeriode.periode
        val delresultat: FordeltKravgrunnlagsbeløp =
            beløpPerPeriode[periode] ?: throw IllegalStateException("Periode i finnes ikke i map beløpPerPeriode")

        return Beregningsresultatsperiode(
            periode = periode.toDatoperiode(),
            feilutbetaltBeløp = delresultat.feilutbetaltBeløp,
            riktigYtelsesbeløp = delresultat.riktigYtelsesbeløp,
            utbetaltYtelsesbeløp = delresultat.utbetaltYtelsesbeløp,
            tilbakekrevingsbeløp = BigDecimal.ZERO,
            tilbakekrevingsbeløpUtenRenter = BigDecimal.ZERO,
            rentebeløp = BigDecimal.ZERO,
            andelAvBeløp = BigDecimal.ZERO,
            vurdering = AnnenVurdering.FORELDET,
            skattebeløp = BigDecimal.ZERO,
            tilbakekrevingsbeløpEtterSkatt = BigDecimal.ZERO,
        )
    }

    private fun skalBeregneRenter(fagområdekode: Fagområdekode): Boolean =
        when (fagområdekode) {
            Fagområdekode.BA, Fagområdekode.KS -> false
            else -> true
        }

    private fun bestemVedtakResultat(
        behandlingId: UUID,
        tilbakekrevingsbeløp: BigDecimal,
        feilutbetaltBeløp: BigDecimal?,
    ): Vedtaksresultat {
        val behandling: Behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP == behandling.saksbehandlingstype) {
            return Vedtaksresultat.INGEN_TILBAKEBETALING
        }
        if (tilbakekrevingsbeløp.compareTo(BigDecimal.ZERO) == 0) {
            return Vedtaksresultat.INGEN_TILBAKEBETALING
        }
        if (tilbakekrevingsbeløp < feilutbetaltBeløp) {
            return Vedtaksresultat.DELVIS_TILBAKEBETALING
        }
        return Vedtaksresultat.FULL_TILBAKEBETALING
    }
}
