package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Kravgrunnlagsperiode432
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface Kravgrunnlagsperiode432Repository : RepositoryInterface<Kravgrunnlagsperiode432, UUID>,
                                              InsertUpdateRepository<Kravgrunnlagsperiode432>