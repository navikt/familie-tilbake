package no.nav.familie.tilbake.repository.tbd

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.domain.tbd.VurdertForeldelse
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
@Transactional
interface VurdertForeldelseRepository : RepositoryInterface<VurdertForeldelse, UUID>, InsertUpdateRepository<VurdertForeldelse> {

    fun findByBehandlingId(behandlingId: UUID): VurdertForeldelse?
}