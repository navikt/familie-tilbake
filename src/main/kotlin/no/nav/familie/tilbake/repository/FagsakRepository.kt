package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Fagsak
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface FagsakRepository : RepositoryInterface<Fagsak, UUID>, InsertUpdateRepository<Fagsak>