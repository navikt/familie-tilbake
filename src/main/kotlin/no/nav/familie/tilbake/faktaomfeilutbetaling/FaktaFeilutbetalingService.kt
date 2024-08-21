package no.nav.familie.tilbake.faktaomfeilutbetaling

import no.nav.familie.kontrakter.felles.Månedsperiode
import no.nav.familie.tilbake.api.dto.BehandlingsstegFaktaDto
import no.nav.familie.tilbake.api.dto.FaktaFeilutbetalingDto
import no.nav.familie.tilbake.api.dto.VurderingAvBrukersUttalelseDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.config.Constants.hentAutomatiskSaksbehandlingBegrunnelse
import no.nav.familie.tilbake.config.FeatureToggleConfig
import no.nav.familie.tilbake.config.FeatureToggleService
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetalingsperiode
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.HarBrukerUttaltSeg
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.VurderingAvBrukersUttalelse
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FaktaFeilutbetalingService(
    private val behandlingRepository: BehandlingRepository,
    private val faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository,
    private val kravgrunnlagRepository: KravgrunnlagRepository,
    private val featureToggleService: FeatureToggleService,
) {
    @Transactional(readOnly = true)
    fun hentFaktaomfeilutbetaling(behandlingId: UUID): FaktaFeilutbetalingDto {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val faktaFeilutbetaling = hentAktivFaktaOmFeilutbetaling(behandlingId)
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        return FaktaFeilutbetalingMapper
            .tilRespons(
                faktaFeilutbetaling = faktaFeilutbetaling,
                kravgrunnlag = kravgrunnlag,
                behandling = behandling,
            )
    }

    @Transactional(readOnly = true)
    fun hentInaktivFaktaomfeilutbetaling(behandlingId: UUID): List<FaktaFeilutbetalingDto> {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val faktaFeilutbetaling: List<FaktaFeilutbetaling> = faktaFeilutbetalingRepository.findByBehandlingId(behandlingId).filter { !it.aktiv }
        val alleKravgrunnlag: List<Kravgrunnlag431> = kravgrunnlagRepository.findByBehandlingId(behandlingId)

        return faktaFeilutbetaling.map { gjeldendeFakta ->
            val kravgrunnlag = alleKravgrunnlag.sortedBy { it.sporbar.opprettetTid }.filter { !it.sperret && !it.avsluttet }.last { it.sporbar.opprettetTid <= gjeldendeFakta.sporbar.opprettetTid }
            FaktaFeilutbetalingMapper.tilRespons(
                faktaFeilutbetaling = gjeldendeFakta,
                kravgrunnlag = kravgrunnlag,
                behandling = behandling,
            )
        }
    }

    @Transactional
    fun lagreFaktaomfeilutbetaling(
        behandlingId: UUID,
        behandlingsstegFaktaDto: BehandlingsstegFaktaDto,
    ) {
        validerVurderingAvBrukersUttalelse(behandlingsstegFaktaDto.vurderingAvBrukersUttalelse)
        deaktiverEksisterendeFaktaOmFeilutbetaling(behandlingId)

        val feilutbetaltePerioder: Set<FaktaFeilutbetalingsperiode> =
            behandlingsstegFaktaDto.feilutbetaltePerioder.map {
                FaktaFeilutbetalingsperiode(
                    periode = Månedsperiode(it.periode.fom, it.periode.tom),
                    hendelsestype = it.hendelsestype,
                    hendelsesundertype = it.hendelsesundertype,
                )
            }.toSet()

        faktaFeilutbetalingRepository.insert(
            FaktaFeilutbetaling(
                behandlingId = behandlingId,
                perioder = feilutbetaltePerioder,
                begrunnelse = behandlingsstegFaktaDto.begrunnelse,
                vurderingAvBrukersUttalelse =
                    behandlingsstegFaktaDto.vurderingAvBrukersUttalelse?.let {
                        VurderingAvBrukersUttalelse(harBrukerUttaltSeg = it.harBrukerUttaltSeg, beskrivelse = it.beskrivelse)
                    },
            ),
        )
    }

    private fun validerVurderingAvBrukersUttalelse(vurderingAvBrukersUttalelse: VurderingAvBrukersUttalelseDto?) {
        vurderingAvBrukersUttalelse?.let {
            if (!featureToggleService.isEnabled(FeatureToggleConfig.VURDERING_AV_BRUKERS_UTTALELSE) && it.harBrukerUttaltSeg != HarBrukerUttaltSeg.IKKE_VURDERT) {
                throw Feil("Feature toggle for vurdering av brukers uttalelse er ikke skrudd på")
            }

            if (it.harBrukerUttaltSeg == HarBrukerUttaltSeg.JA && it.beskrivelse.isNullOrBlank()) {
                throw Feil("Mangler beskrivelse på vurdering av brukers uttalelse")
            } else if (it.harBrukerUttaltSeg != HarBrukerUttaltSeg.JA && !it.beskrivelse.isNullOrBlank()) {
                throw Feil("Skal ikke ha beskrivelse når bruker ikke har uttalt seg")
            }
        }
    }

    @Transactional
    fun lagreFastFaktaForAutomatiskSaksbehandling(behandlingId: UUID) {
        val feilutbetaltePerioder =
            hentFaktaomfeilutbetaling(behandlingId).feilutbetaltePerioder.map {
                FaktaFeilutbetalingsperiode(
                    periode = it.periode.toMånedsperiode(),
                    hendelsestype = Hendelsestype.ANNET,
                    hendelsesundertype = Hendelsesundertype.ANNET_FRITEKST,
                )
            }.toSet()
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        faktaFeilutbetalingRepository.insert(
            FaktaFeilutbetaling(
                behandlingId = behandlingId,
                perioder = feilutbetaltePerioder,
                begrunnelse = hentAutomatiskSaksbehandlingBegrunnelse(behandling),
            ),
        )
    }

    fun hentAktivFaktaOmFeilutbetaling(behandlingId: UUID): FaktaFeilutbetaling? {
        return faktaFeilutbetalingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
    }

    fun hentAlleFaktaOmFeilutbetaling(behandlingId: UUID): List<FaktaFeilutbetaling> {
        return faktaFeilutbetalingRepository.findByBehandlingId(behandlingId)
    }

    @Transactional
    fun deaktiverEksisterendeFaktaOmFeilutbetaling(behandlingId: UUID) {
        faktaFeilutbetalingRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)?.copy(aktiv = false)?.let {
            faktaFeilutbetalingRepository.update(it.copy(vurderingAvBrukersUttalelse = it.vurderingAvBrukersUttalelse?.copy(aktiv = false)))
        }
    }
}
