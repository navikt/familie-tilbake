package no.nav.familie.tilbake.repository.tbd

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.domain.tbd.GrupperingVurdertForeldelse
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface GrupperingVurdertForeldelseRepository : RepositoryInterface<GrupperingVurdertForeldelse, UUID>,
                                                  InsertUpdateRepository<GrupperingVurdertForeldelse>