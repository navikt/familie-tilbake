package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.EksternBehandling
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface EksternBehandlingRepository : RepositoryInterface<EksternBehandling, UUID>, InsertUpdateRepository<EksternBehandling>