package no.nav.familie.tilbake.foreldelse

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.foreldelse.domain.Foreldelsesperiode
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ForeldelsesperiodeRepository : RepositoryInterface<Foreldelsesperiode, UUID>, InsertUpdateRepository<Foreldelsesperiode>
