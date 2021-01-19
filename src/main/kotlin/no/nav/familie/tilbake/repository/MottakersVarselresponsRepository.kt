package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.MottakersVarselrespons
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface MottakersVarselresponsRepository : RepositoryInterface<MottakersVarselrespons, UUID>,
                                             InsertUpdateRepository<MottakersVarselrespons>