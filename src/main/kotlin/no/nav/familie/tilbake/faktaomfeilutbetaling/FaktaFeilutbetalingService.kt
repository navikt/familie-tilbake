package no.nav.familie.tilbake.faktaomfeilutbetaling

import no.nav.familie.tilbake.api.dto.FaktaFeilutbetalingDto
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
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
        val faktaFeilutbetaling = faktaFeilutbetalingRepository.findByAktivIsTrueAndBehandlingId(behandlingId)
        val kravgrunnlag = kravgrunnlagRepository.findByBehandlingIdAndAktivIsTrue(behandlingId)
        return FaktaFeilutbetalingMapper.tilRespons(faktaFeilutbetaling = faktaFeilutbetaling,
                                                    kravgrunnlag = kravgrunnlag,
                                                    revurderingsvedtaksdato = behandling.aktivtFagsystem.revurderingsvedtaksdato,
                                                    varsletData = behandling.aktivtVarsel,
                                                    fagsystemsbehandling = behandling.aktivtFagsystem)
    }
}
