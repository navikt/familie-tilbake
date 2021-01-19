package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Behandling
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BehandlingRepository : RepositoryInterface<Behandling, UUID>, InsertUpdateRepository<Behandling>