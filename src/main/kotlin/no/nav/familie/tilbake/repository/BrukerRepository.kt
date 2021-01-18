package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Bruker
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BrukerRepository : RepositoryInterface<Bruker, UUID>, InsertUpdateRepository<Bruker>