package no.nav.familie.tilbake.beregning

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Saksbehandlingstype
import no.nav.familie.tilbake.beregning.modell.BeregningResultat
import no.nav.familie.tilbake.beregning.modell.BeregningResultatPeriode
import no.nav.familie.tilbake.beregning.modell.FordeltKravgrunnlagBeløp
import no.nav.familie.tilbake.beregning.modell.GrunnlagPeriodeMedSkattProsent
import no.nav.familie.tilbake.beregning.modell.Vedtaksresultat
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.domain.tbd.AnnenVurdering
import no.nav.familie.tilbake.domain.tbd.Foreldelsesperiode
import no.nav.familie.tilbake.domain.tbd.Foreldelsesvurderingstype
import no.nav.familie.tilbake.domain.tbd.Vilkårsvurdering
import no.nav.familie.tilbake.domain.tbd.Vilkårsvurderingsperiode
import no.nav.familie.tilbake.domain.tbd.VurdertForeldelse
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import no.nav.familie.tilbake.repository.tbd.VilkårsvurderingRepository
import no.nav.familie.tilbake.repository.tbd.VurdertForeldelseRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class TilbakekrevingBeregningTjeneste(private var kravgrunnlagRepository: KravgrunnlagRepository,
                                      private var vurdertForeldelseRepository: VurdertForeldelseRepository,
                                      private var vilkårsvurderingRepository: VilkårsvurderingRepository,
                                      private var behandlingRepository: BehandlingRepository,
                                      private var kravgrunnlagBeregningTjeneste: KravgrunnlagBeregningService) {

    fun beregn(behandlingId: UUID): BeregningResultat {
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        val vurdertForeldelse = hentVurdertForeldelse(behandlingId)
        val vilkårsvurdering = hentVilkårsvurdering(behandlingId)
        val vurderingsperioder: List<Periode> = finnPerioder(vurdertForeldelse, vilkårsvurdering)
        val perioderMedBeløp: Map<Periode, FordeltKravgrunnlagBeløp> =
                kravgrunnlagBeregningTjeneste.fordelKravgrunnlagBeløpPåPerioder(kravgrunnlag, vurderingsperioder)
        val beregningResultatPerioder =
                beregn(kravgrunnlag, vurdertForeldelse, vilkårsvurdering, perioderMedBeløp, true)
        val totalTilbakekrevingBeløp = beregningResultatPerioder.sumOf { it.tilbakekrevingBeløp }
        val totalFeilutbetaltBeløp = beregningResultatPerioder.sumOf { it.feilutbetaltBeløp }
        return BeregningResultat(vedtaksresultat = bestemVedtakResultat(behandlingId,
                                                                        totalTilbakekrevingBeløp,
                                                                        totalFeilutbetaltBeløp),
                                 beregningResultatPerioder = (beregningResultatPerioder))
    }

    private fun hentVilkårsvurdering(behandlingId: UUID): Vilkårsvurdering? {
        return vilkårsvurderingRepository.findByBehandlingId(behandlingId)
    }

    private fun hentVurdertForeldelse(behandlingId: UUID): VurdertForeldelse? {
        return vurdertForeldelseRepository.findByBehandlingId(behandlingId)
    }

    private fun finnPerioder(vurdertForeldelse: VurdertForeldelse?, vilkårsvurdering: Vilkårsvurdering?): List<Periode> {
        return finnForeldedePerioder(vurdertForeldelse) + finnIkkeForeldedePerioder(vilkårsvurdering)
    }

    private fun beregn(kravgrunnlag: Kravgrunnlag431,
                       vurdertForeldelse: VurdertForeldelse?,
                       vilkårsvurdering: Vilkårsvurdering?,
                       perioderMedBeløp: Map<Periode, FordeltKravgrunnlagBeløp>,
                       beregnRenter: Boolean): List<BeregningResultatPeriode> {
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
                                               kravbeløpPerPeriode: Map<Periode, FordeltKravgrunnlagBeløp>,
                                               beregnRenter: Boolean): Collection<BeregningResultatPeriode> {
        return vilkårsvurdering?.perioder
                       ?.map { beregnIkkeForeldetPeriode(kravgrunnlag, it, kravbeløpPerPeriode, beregnRenter) }
               ?: emptyList()
    }

    private fun beregnForForeldedePerioder(vurdertForeldelse: VurdertForeldelse?,
                                           kravbeløpPerPeriode: Map<Periode, FordeltKravgrunnlagBeløp>)
            : Collection<BeregningResultatPeriode> {
        return vurdertForeldelse?.foreldelsesperioder
                       ?.filter { Foreldelsesvurderingstype.FORELDET == it.foreldelsesvurderingstype }
                       ?.map { beregnForeldetPeriode(kravbeløpPerPeriode, it) }
               ?: emptyList()
    }

    private fun beregnForeldetPeriode(beløpPerPeriode: Map<Periode, FordeltKravgrunnlagBeløp>,
                                      foreldelsePeriode: Foreldelsesperiode): BeregningResultatPeriode {
        val periode: Periode = foreldelsePeriode.periode
        val delresultat: FordeltKravgrunnlagBeløp =
                beløpPerPeriode[periode] ?: throw IllegalStateException("Periode i finnes ikke i map beløpPerPeriode")

        return BeregningResultatPeriode(periode = periode,
                                        feilutbetaltBeløp = delresultat.feilutbetaltBeløp,
                                        riktigYtelseBeløp = delresultat.riktigYtelseBeløp,
                                        utbetaltYtelseBeløp = delresultat.utbetaltYtelseBeløp,
                                        tilbakekrevingBeløp = BigDecimal.ZERO,
                                        tilbakekrevingBeløpUtenRenter = BigDecimal.ZERO,
                                        renteBeløp = BigDecimal.ZERO,
                                        andelAvBeløp = BigDecimal.ZERO,
                                        vurdering = AnnenVurdering.FORELDET,
                                        skattBeløp = BigDecimal.ZERO,
                                        tilbakekrevingBeløpEtterSkatt = BigDecimal.ZERO)
    }

    private fun beregnIkkeForeldetPeriode(kravgrunnlag: Kravgrunnlag431,
                                          vurdering: Vilkårsvurderingsperiode,
                                          kravbeløpPerPeriode: Map<Periode, FordeltKravgrunnlagBeløp>,
                                          beregnRenter: Boolean): BeregningResultatPeriode {
        val delresultat = kravbeløpPerPeriode[vurdering.periode]
                          ?: throw IllegalStateException("Periode i finnes ikke i map kravbeløpPerPeriode")
        val perioderMedSkattProsent = lagGrunnlagPeriodeMedSkattProsent(vurdering.periode, kravgrunnlag)
        return TilbakekrevingBeregnerVilkår.beregn(vurdering, delresultat, perioderMedSkattProsent, beregnRenter)
    }

    private fun lagGrunnlagPeriodeMedSkattProsent(vurderingsperiode: Periode,
                                                  kravgrunnlag: Kravgrunnlag431): List<GrunnlagPeriodeMedSkattProsent> {
        return kravgrunnlag.perioder
                .sortedBy { it.periode.fom }
                .map {
                    it.beløp.map { kgBeløp ->
                        val maksTilbakekrevesBeløp: BigDecimal =
                                BeregnBeløpUtil.beregnBeløpForPeriode(kgBeløp.tilbakekrevesBeløp,
                                                                      vurderingsperiode,
                                                                      it.periode)
                        GrunnlagPeriodeMedSkattProsent(it.periode, maksTilbakekrevesBeløp, kgBeløp.skatteprosent)
                    }
                }.flatten()
    }

    private fun bestemVedtakResultat(behandlingId: UUID,
                                     tilbakekrevingBeløp: BigDecimal,
                                     feilutbetaltBeløp: BigDecimal?): Vedtaksresultat {
        val behandling: Behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        if (Saksbehandlingstype.AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP == behandling.saksbehandlingstype) {
            return Vedtaksresultat.INGEN_TILBAKEBETALING
        }
        if (tilbakekrevingBeløp.compareTo(BigDecimal.ZERO) == 0) {
            return Vedtaksresultat.INGEN_TILBAKEBETALING
        }
        if (tilbakekrevingBeløp < feilutbetaltBeløp) {
            return Vedtaksresultat.DELVIS_TILBAKEBETALING
        }
        return Vedtaksresultat.FULL_TILBAKEBETALING
    }

}