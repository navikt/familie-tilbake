package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Foreldelsesperiode
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ForeldelsesperiodeRepository : RepositoryInterface<Foreldelsesperiode, UUID>, InsertUpdateRepository<Foreldelsesperiode>