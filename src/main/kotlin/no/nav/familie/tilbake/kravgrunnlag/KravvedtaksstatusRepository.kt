package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravvedtaksstatus437
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
@Transactional
interface KravvedtaksstatusRepository : RepositoryInterface<Kravvedtaksstatus437, UUID>,
                                        InsertUpdateRepository<Kravvedtaksstatus437>
