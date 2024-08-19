package no.nav.familie.tilbake.vilkårsvurdering

import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.api.dto.AktsomhetDto
import no.nav.familie.tilbake.api.dto.BehandlingsstegVilkårsvurderingDto
import no.nav.familie.tilbake.api.dto.VilkårsvurderingsperiodeDto
import no.nav.familie.tilbake.api.dto.VurdertVilkårsvurderingDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants.hentAutomatiskVilkårsvurderingAktsomhetBegrunnelse
import no.nav.familie.tilbake.config.Constants.hentAutomatiskVilkårsvurderingBegrunnelse
import no.nav.familie.tilbake.faktaomfeilutbetaling.FaktaFeilutbetalingService
import no.nav.familie.tilbake.foreldelse.ForeldelseService
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.vilkårsvurdering.domain.Aktsomhet
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurdering
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurderingsresultat
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.foreldelse.domain.VurdertForeldelse
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431

@Service
class VilkårsvurderingService(
    val vilkårsvurderingRepository: VilkårsvurderingRepository,
    val kravgrunnlagRepository: KravgrunnlagRepository,
    val fagsakRepository: FagsakRepository,
    val behandlingRepository: BehandlingRepository,
    val foreldelseService: ForeldelseService,
    val faktaFeilutbetalingService: FaktaFeilutbetalingService,
) {
    fun hentVilkårsvurdering(behandlingId: UUID): VurdertVilkårsvurderingDto {
        val faktaOmFeilutbetaling =
            faktaFeilutbetalingService.hentAktivFaktaOmFeilutbetaling(behandlingId)
                ?: throw Feil(
                    message =
                        "Fakta om feilutbetaling finnes ikke for behandling=$behandlingId, " +
                            "kan ikke hente vilkårsvurdering",
                )
        val kravgrunnlag431 = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        val vilkårsvurdering = vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        val vurdertForeldelse = foreldelseService.hentAktivVurdertForeldelse(behandlingId)
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
        kravgrunnlag431: Kravgrunnlag431
    ): VurdertVilkårsvurderingDto {
        val perioder = mutableListOf<Månedsperiode>()
        val foreldetPerioderMedBegrunnelse = mutableMapOf<Månedsperiode, String>()
        if (vurdertForeldelse == null) {
            // fakta perioder
            faktaOmFeilutbetaling.perioder
                .filter { !erPeriodeAlleredeVurdert(vilkårsvurdering, it.periode) }
                .forEach { perioder.add(it.periode) }
        } else {
            // Ikke foreldet perioder uten perioder som allerede vurdert i vilkårsvurdering
            vurdertForeldelse.foreldelsesperioder
                .filter { !it.erForeldet() }
                .filter { !erPeriodeAlleredeVurdert(vilkårsvurdering, it.periode) }
                .forEach { perioder.add(it.periode) }
            // foreldet perioder
            vurdertForeldelse.foreldelsesperioder
                .filter { it.erForeldet() }
                .forEach { foreldetPerioderMedBegrunnelse[it.periode] = it.begrunnelse }
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
        val fagsystem = fagsakRepository.findByIdOrThrow(behandling.fagsakId).fagsystem
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        // Valider request
        VilkårsvurderingValidator.validerVilkårsvurdering(
            vilkårsvurderingDto = behandlingsstegVilkårsvurderingDto,
            kravgrunnlag431 = kravgrunnlag,
        )
        // filter bort perioder som er foreldet
        val ikkeForeldetPerioder =
            behandlingsstegVilkårsvurderingDto.vilkårsvurderingsperioder
                .filter { !foreldelseService.erPeriodeForeldet(behandlingId, Månedsperiode(it.periode.fom, it.periode.tom)) }
        deaktiverEksisterendeVilkårsvurdering(behandlingId)
        vilkårsvurderingRepository.insert(
            VilkårsvurderingMapper.tilDomene(
                behandlingId = behandlingId,
                vilkårsvurderingsperioder = ikkeForeldetPerioder,
                fagsystem = fagsystem,
            ),
        )
    }

    @Transactional
    fun lagreFastVilkårForAutomatiskSaksbehandling(behandlingId: UUID) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val fagsystem = fagsakRepository.findByIdOrThrow(behandling.fagsakId).fagsystem

        val perioder = hentVilkårsvurdering(behandlingId).perioder
        val vurdertePerioder =
            perioder.filter { !it.foreldet }.map {
                VilkårsvurderingsperiodeDto(
                    periode = it.periode,
                    vilkårsvurderingsresultat = Vilkårsvurderingsresultat.FORSTO_BURDE_FORSTÅTT,
                    begrunnelse = hentAutomatiskVilkårsvurderingBegrunnelse(behandling.saksbehandlingstype),
                    aktsomhetDto =
                        AktsomhetDto(
                            aktsomhet = Aktsomhet.SIMPEL_UAKTSOMHET,
                            tilbakekrevSmåbeløp = false,
                            begrunnelse = hentAutomatiskVilkårsvurderingAktsomhetBegrunnelse(behandling.saksbehandlingstype),
                        ),
                )
            }
        vilkårsvurderingRepository.insert(
            VilkårsvurderingMapper.tilDomene(
                behandlingId = behandlingId,
                vilkårsvurderingsperioder = vurdertePerioder,
                fagsystem = fagsystem,
            ),
        )
    }

    @Transactional
    fun deaktiverEksisterendeVilkårsvurdering(behandlingId: UUID) {
        vilkårsvurderingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)?.copy(aktiv = false)?.let {
            vilkårsvurderingRepository.update(it)
        }
    }

    private fun erPeriodeAlleredeVurdert(
        vilkårsvurdering: Vilkårsvurdering?,
        periode: Månedsperiode,
    ): Boolean = vilkårsvurdering?.perioder?.any { periode.inneholder(it.periode) } == true
}
