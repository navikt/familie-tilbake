package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Behandlingsstegstilstand
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BehandlingsstegstilstandRepository : RepositoryInterface<Behandlingsstegstilstand, UUID>,
                                               InsertUpdateRepository<Behandlingsstegstilstand>