package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Totrinnsvurdering
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TotrinnsvurderingRepository : RepositoryInterface<Totrinnsvurdering, UUID>, InsertUpdateRepository<Totrinnsvurdering>