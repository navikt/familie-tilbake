package no.nav.familie.tilbake.repository.tbd

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.domain.tbd.MottakersVarselrespons
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface MottakersVarselresponsRepository : RepositoryInterface<MottakersVarselrespons, UUID>,
                                             InsertUpdateRepository<MottakersVarselrespons>