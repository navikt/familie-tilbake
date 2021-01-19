package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Behandlingsstegstype
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BehandlingsstegstypeRepository : RepositoryInterface<Behandlingsstegstype, UUID>,
                                           InsertUpdateRepository<Behandlingsstegstype>