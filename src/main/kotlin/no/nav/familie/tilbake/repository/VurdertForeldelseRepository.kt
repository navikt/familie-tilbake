package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.VurdertForeldelse
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VurdertForeldelseRepository : RepositoryInterface<VurdertForeldelse, UUID>, InsertUpdateRepository<VurdertForeldelse>