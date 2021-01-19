package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.GrupperingVerge
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface GrupperingVergeRepository : RepositoryInterface<GrupperingVerge, UUID>, InsertUpdateRepository<GrupperingVerge>
