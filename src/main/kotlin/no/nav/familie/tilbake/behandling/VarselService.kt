package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.common.repository.findByIdOrThrow
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class VarselService(val behandlingRepository: BehandlingRepository) {

    fun lagre(behandlingId: UUID, varseltekst: String, varselbeløp: Long) {
        val behandling = behandlingRepository.findByIdOrThrow(behandlingId)
        val copy = behandling.copy(varsler = behandling.varsler + Varsel(varseltekst = varseltekst, varselbeløp = varselbeløp))
        behandlingRepository.update(copy)
    }
}
