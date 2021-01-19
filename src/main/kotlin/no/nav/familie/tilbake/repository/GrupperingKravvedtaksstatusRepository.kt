package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.GrupperingKravvedtaksstatus
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface GrupperingKravvedtaksstatusRepository : RepositoryInterface<GrupperingKravvedtaksstatus, UUID>,
                                                  InsertUpdateRepository<GrupperingKravvedtaksstatus>