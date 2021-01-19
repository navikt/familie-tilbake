package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Verge
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VergeRepository : RepositoryInterface<Verge, UUID>, InsertUpdateRepository<Verge>