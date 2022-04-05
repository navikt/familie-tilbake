package no.nav.familie.tilbake.dokumentbestilling.vedtak

import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VedtaksbrevgunnlagService(val behandlingRepository: BehandlingRepository,
                                val vedtaksbrevgrunnlagRepository: VedtaksbrevgrunnlagRepository) {


    fun hentVedtaksbrevgrunnlag(behandlingId: UUID): Vedtaksbrevgrunnlag {
        val fagsakId = vedtaksbrevgrunnlagRepository.finnFagsakIdForBehandlingId(behandlingId)
        return vedtaksbrevgrunnlagRepository.findByIdOrThrow(fagsakId)
    }

}