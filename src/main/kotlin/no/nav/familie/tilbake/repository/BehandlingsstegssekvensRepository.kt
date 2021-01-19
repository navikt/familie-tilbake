package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Behandlingsstegssekvens
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BehandlingsstegssekvensRepository : RepositoryInterface<Behandlingsstegssekvens, UUID>,
                                              InsertUpdateRepository<Behandlingsstegssekvens>