package no.nav.familie.tilbake.totrinn

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.totrinn.domain.Totrinnsresultatsgrunnlag
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface TotrinnsresultatsgrunnlagRepository : RepositoryInterface<Totrinnsresultatsgrunnlag, UUID>,
                                                InsertUpdateRepository<Totrinnsresultatsgrunnlag> {

    fun findByBehandlingIdAndAktivIsTrue(behandlingId: UUID): Totrinnsresultatsgrunnlag?
}
