package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Aksjonspunktsdefinisjon
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface AksjonspunktsdefinisjonRepository : RepositoryInterface<Aksjonspunktsdefinisjon, UUID>,
                                              InsertUpdateRepository<Aksjonspunktsdefinisjon>