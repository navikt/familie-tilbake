package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
@Transactional
interface KravgrunnlagRepository : RepositoryInterface<Kravgrunnlag431, UUID>,
                                   InsertUpdateRepository<Kravgrunnlag431> {

      fun findByBehandlingIdAndAktivIsTrue(behandlingId:UUID): Kravgrunnlag431

      fun existsByBehandlingIdAndAktivTrueAndSperretFalse(behandlingId: UUID): Boolean

}
