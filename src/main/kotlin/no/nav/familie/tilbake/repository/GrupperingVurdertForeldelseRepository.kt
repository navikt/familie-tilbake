package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.GrupperingVurdertForeldelse
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface GrupperingVurdertForeldelseRepository : RepositoryInterface<GrupperingVurdertForeldelse, UUID>,
                                                  InsertUpdateRepository<GrupperingVurdertForeldelse>