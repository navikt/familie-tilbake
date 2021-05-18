package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface VarselRepository : RepositoryInterface<Varsel, UUID>, InsertUpdateRepository<Varsel>
