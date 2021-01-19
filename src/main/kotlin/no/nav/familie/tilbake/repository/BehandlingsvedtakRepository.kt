package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Behandlingsvedtak
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface BehandlingsvedtakRepository : RepositoryInterface<Behandlingsvedtak, UUID>, InsertUpdateRepository<Behandlingsvedtak>