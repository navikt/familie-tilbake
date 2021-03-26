package no.nav.familie.tilbake.beregning

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Saksbehandlingstype
import no.nav.familie.tilbake.beregning.modell.Beregningsresultat
import no.nav.familie.tilbake.beregning.modell.Beregningsresultatsperiode
import no.nav.familie.tilbake.beregning.modell.FordeltKravgrunnlagsbeløp
import no.nav.familie.tilbake.beregning.modell.GrunnlagsperiodeMedSkatteprosent
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.domain.tbd.AnnenVurdering
import no.nav.familie.tilbake.domain.tbd.Vilkårsvurdering
import no.nav.familie.tilbake.domain.tbd.Vilkårsvurderingsperiode
import no.nav.familie.tilbake.foreldelse.VurdertForeldelseRepository
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesperiode
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesvurderingstype
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.repository.tbd.VilkårsvurderingRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class TilbakekrevingsberegningService(private var kravgrunnlagRepository: KravgrunnlagRepository,
                                      private var vurdertForeldelseRepository: VurdertForeldelseRepository,
                                      private var vilkårsvurderingRepository: VilkårsvurderingRepository,
                                      private var behandlingRepository: BehandlingRepository,
                                      private var kravgrunnlagsberegningService: KravgrunnlagsberegningService) {

    fun beregn(behandlingId: UUID): Beregningsresultat {
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        val vurdertForeldelse = hentVurdertForeldelse(behandlingId)
        val vilkårsvurdering = hentVilkårsvurdering(behandlingId)
        val vurderingsperioder: List<Periode> = finnPerioder(vurdertForeldelse, vilkårsvurdering)
        val perioderMedBeløp: Map<Periode, FordeltKravgrunnlagsbeløp> =
                kravgrunnlagsberegningService.fordelKravgrunnlagBeløpPåPerioder(kravgrunnlag, vurderingsperioder)
        val beregningsresultatperioder =
                beregn(kravgrunnlag, vurdertForeldelse, vilkårsvurdering, perioderMedBeløp, true)
        val totalTilbakekrevingsbeløp = beregningsresultatperioder.sumOf { it.tilbakekrevingsbeløp }
        val totalFeilutbetaltBeløp = beregningsresultatperioder.sumOf { it.feilutbetaltBeløp }
        return Beregningsresultat(vedtaksresultat = bestemVedtakResultat(behandlingId,
                                                                         totalTilbakekrevingsbeløp,
                                                                         totalFeilutbetaltBeløp),
                                  beregningsresultatsperioder = (beregningsresultatperioder))
    }

    private fun hentVilkårsvurdering(behandlingId: UUID): Vilkårsvurdering? {
        return vilkårsvurderingRepository.findByBehandlingId(behandlingId)
    }

    private fun hentVurdertForeldelse(behandlingId: UUID): VurdertForeldelse? {
        return vurdertForeldelseRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
    }

    private fun finnPerioder(vurdertForeldelse: VurdertForeldelse?, vilkårsvurdering: Vilkårsvurdering?): List<Periode> {
        return finnForeldedePerioder(vurdertForeldelse) + finnIkkeForeldedePerioder(vilkårsvurdering)
    }

    private fun beregn(kravgrunnlag: Kravgrunnlag431,
                       vurdertForeldelse: VurdertForeldelse?,
                       vilkårsvurdering: Vilkårsvurdering?,
                       perioderMedBeløp: Map<Periode, FordeltKravgrunnlagsbeløp>,
                       beregnRenter: Boolean): List<Beregningsresultatsperiode> {
        return (beregnForForeldedePerioder(vurdertForeldelse, perioderMedBeløp) +
                beregnForIkkeForeldedePerioder(kravgrunnlag, vilkårsvurdering, perioderMedBeløp, beregnRenter))
                .sortedBy { it.periode.fom }
    }

    private fun finnIkkeForeldedePerioder(vilkårsvurdering: Vilkårsvurdering?): List<Periode> {
        return vilkårsvurdering?.perioder?.map(Vilkårsvurderingsperiode::periode)
               ?: emptyList()
    }

    private fun finnForeldedePerioder(vurdertForeldelse: VurdertForeldelse?): List<Periode> {
        return vurdertForeldelse?.foreldelsesperioder
                       ?.filter(Foreldelsesperiode::erForeldet)
                       ?.map(Foreldelsesperiode::periode)
               ?: emptyList()
    }

    private fun beregnForIkkeForeldedePerioder(kravgrunnlag: Kravgrunnlag431,
                                               vilkårsvurdering: Vilkårsvurdering?,
                                               kravbeløpPerPeriode: Map<Periode, FordeltKravgrunnlagsbeløp>,
                                               beregnRenter: Boolean): Collection<Beregningsresultatsperiode> {
        return vilkårsvurdering?.perioder
                       ?.map { beregnIkkeForeldetPeriode(kravgrunnlag, it, kravbeløpPerPeriode, beregnRenter) }
               ?: emptyList()
    }

    private fun beregnForForeldedePerioder(vurdertForeldelse: VurdertForeldelse?,
                                           kravbeløpPerPeriode: Map<Periode, FordeltKravgrunnlagsbeløp>)
            : Collection<Beregningsresultatsperiode> {
        return vurdertForeldelse?.foreldelsesperioder
                       ?.filter { Foreldelsesvurderingstype.FORELDET == it.foreldelsesvurderingstype }
                       ?.map { beregnForeldetPeriode(kravbeløpPerPeriode, it) }
               ?: emptyList()
    }

    private fun beregnForeldetPeriode(beløpPerPeriode: Map<Periode, FordeltKravgrunnlagsbeløp>,
                                      foreldelsePeriode: Foreldelsesperiode): Beregningsresultatsperiode {
        val periode: Periode = foreldelsePeriode.periode
        val delresultat: FordeltKravgrunnlagsbeløp =
                beløpPerPeriode[periode] ?: throw IllegalStateException("Periode i finnes ikke i map beløpPerPeriode")

        return Beregningsresultatsperiode(periode = periode,
                                          feilutbetaltBeløp = delresultat.feilutbetaltBeløp,
                                          riktigYtelsesbeløp = delresultat.riktigYtelsesbeløp,
                                          utbetaltYtelsesbeløp = delresultat.utbetaltYtelsesbeløp,
                                          tilbakekrevingsbeløp = BigDecimal.ZERO,
                                          tilbakekrevingsbeløpUtenRenter = BigDecimal.ZERO,
                                          rentebeløp = BigDecimal.ZERO,
                                          andelAvBeløp = BigDecimal.ZERO,
                                          vurdering = AnnenVurdering.FORELDET,
                                          skattebeløp = BigDecimal.ZERO,
                                          tilbakekrevingsbeløpEtterSkatt = BigDecimal.ZERO)
    }

    private fun beregnIkkeForeldetPeriode(kravgrunnlag: Kravgrunnlag431,
                                          vurdering: Vilkårsvurderingsperiode,
                                          kravbeløpPerPeriode: Map<Periode, FordeltKravgrunnlagsbeløp>,
                                          beregnRenter: Boolean): Beregningsresultatsperiode {
        val delresultat = kravbeløpPerPeriode[vurdering.periode]
                          ?: throw IllegalStateException("Periode i finnes ikke i map kravbeløpPerPeriode")
        val perioderMedSkattProsent = lagGrunnlagPeriodeMedSkattProsent(vurdering.periode, kravgrunnlag)
        return TilbakekrevingsberegningVilkår.beregn(vurdering, delresultat, perioderMedSkattProsent, beregnRenter)
    }

    private fun lagGrunnlagPeriodeMedSkattProsent(vurderingsperiode: Periode,
                                                  kravgrunnlag: Kravgrunnlag431): List<GrunnlagsperiodeMedSkatteprosent> {
        return kravgrunnlag.perioder
                .sortedBy { it.periode.fom }
                .map {
                    it.beløp.map { kgBeløp ->
                        val maksTilbakekrevesBeløp: BigDecimal =
                                BeløpsberegningUtil.beregnBeløpForPeriode(kgBeløp.tilbakekrevesBeløp,
                                                                          vurderingsperiode,
                                                                          it.periode)
                        GrunnlagsperiodeMedSkatteprosent(it.periode, maksTilbakekrevesBeløp, kgBeløp.skatteprosent)
                    }
                }.flatten()
    }

    private fun bestemVedtakResultat(behandlingId: UUID,
                                     tilbakekrevingsbeløp: BigDecimal,
                                     feilutbetaltBeløp: BigDecimal?): Vedtaksresultat {
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
