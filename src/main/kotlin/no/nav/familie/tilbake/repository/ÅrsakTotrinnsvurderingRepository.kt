package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.ÅrsakTotrinnsvurdering
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface ÅrsakTotrinnsvurderingRepository : RepositoryInterface<ÅrsakTotrinnsvurdering, UUID>,
                                             InsertUpdateRepository<ÅrsakTotrinnsvurdering>