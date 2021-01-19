package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Vurderingspunktsdefinisjon
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface VurderingspunktsdefinisjonRepository : RepositoryInterface<Vurderingspunktsdefinisjon, UUID>,
                                                 InsertUpdateRepository<Vurderingspunktsdefinisjon>