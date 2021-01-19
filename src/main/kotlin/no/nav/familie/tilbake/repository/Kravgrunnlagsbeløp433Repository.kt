package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Kravgrunnlagsbeløp433
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface Kravgrunnlagsbeløp433Repository : RepositoryInterface<Kravgrunnlagsbeløp433, UUID>,
                                            InsertUpdateRepository<Kravgrunnlagsbeløp433>