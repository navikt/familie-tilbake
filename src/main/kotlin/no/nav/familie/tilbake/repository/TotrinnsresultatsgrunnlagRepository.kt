package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.Totrinnsresultatsgrunnlag
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface TotrinnsresultatsgrunnlagRepository : RepositoryInterface<Totrinnsresultatsgrunnlag, UUID>,
                                                InsertUpdateRepository<Totrinnsresultatsgrunnlag>