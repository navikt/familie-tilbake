package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Varsel
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VarselRepository : RepositoryInterface<Varsel, UUID>, InsertUpdateRepository<Varsel>