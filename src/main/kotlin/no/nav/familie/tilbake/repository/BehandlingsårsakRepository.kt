package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Behandlingsårsak
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BehandlingsårsakRepository : RepositoryInterface<Behandlingsårsak, UUID>, InsertUpdateRepository<Behandlingsårsak>