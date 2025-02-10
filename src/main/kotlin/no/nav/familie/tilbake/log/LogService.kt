package no.nav.familie.tilbake.log

import no.nav.familie.tilbake.behandling.FagsakRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class LogService(
    private val fagsakRepository: FagsakRepository,
) {
    fun contextFraBehandling(behandlingId: UUID): SecureLog.Context =
        SecureLog.Context.medBehandling(
            fagsystemId = fagsakRepository.finnFagsakForBehandlingId(behandlingId).eksternFagsakId,
            behandlingId = behandlingId.toString(),
        )
}
