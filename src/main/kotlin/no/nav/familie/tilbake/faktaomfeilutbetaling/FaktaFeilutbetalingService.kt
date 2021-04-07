package no.nav.familie.tilbake.faktaomfeilutbetaling

import no.nav.familie.tilbake.api.dto.BehandlingsstegFaktaDto
import no.nav.familie.tilbake.api.dto.FaktaFeilutbetalingDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetaling
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetalingsperiode
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FaktaFeilutbetalingService(private val behandlingRepository: BehandlingRepository,
                                 private val faktaFeilutbetalingRepository: FaktaFeilutbetalingRepository,
                                 private val kravgrunnlagRepository: KravgrunnlagRepository) {

    @Transactional(readOnly = true)
    fun hentFaktaomfeilutbetaling(behandlingId: UUID): FaktaFeilutbetalingDto {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val faktaFeilutbetaling = hentAktivFaktaOmFeilutbetaling(behandlingId)
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        return FaktaFeilutbetalingMapper
                .tilRespons(faktaFeilutbetaling = faktaFeilutbetaling,
                            kravgrunnlag = kravgrunnlag,
                            revurderingsvedtaksdato = behandling.aktivFagsystemsbehandling.revurderingsvedtaksdato,
                            varsletData = behandling.aktivtVarsel,
                            fagsystemsbehandling = behandling.aktivFagsystemsbehandling)
    }

    @Transactional
    fun lagreFaktaomfeilutbetaling(behandlingId: UUID, behandlingsstegFaktaDto: BehandlingsstegFaktaDto) {
        val eksisterendeFaktaData: FaktaFeilutbetaling? = hentAktivFaktaOmFeilutbetaling(behandlingId)
        if (eksisterendeFaktaData != null) {
            faktaFeilutbetalingRepository.update(eksisterendeFaktaData.copy(aktiv = false))
        }

        val feilutbetaltePerioder: Set<FaktaFeilutbetalingsperiode> = behandlingsstegFaktaDto.feilutbetaltePerioder.map {
            FaktaFeilutbetalingsperiode(periode = Periode(it.periode.fom, it.periode.tom),
                                        hendelsestype = it.hendelsestype,
                                        hendelsesundertype = it.hendelsesundertype)
        }.toSet()

        faktaFeilutbetalingRepository.insert(FaktaFeilutbetaling(behandlingId = behandlingId,
                                                                 perioder = feilutbetaltePerioder,
                                                                 begrunnelse = behandlingsstegFaktaDto.begrunnelse))
    }

    fun hentAktivFaktaOmFeilutbetaling(behandlingId: UUID): FaktaFeilutbetaling? {
        return faktaFeilutbetalingRepository.findByAktivIsTrueAndBehandlingId(behandlingId)
    }
}
