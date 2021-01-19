package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.FaktaFeilutbetalingsperiode
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface FaktaFeilutbetalingsperiodeRepository : RepositoryInterface<FaktaFeilutbetalingsperiode, UUID>,
                                                  InsertUpdateRepository<FaktaFeilutbetalingsperiode>