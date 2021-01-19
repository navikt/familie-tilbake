package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.FaktaFeilutbetaling
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface FaktaFeilutbetalingRepository : RepositoryInterface<FaktaFeilutbetaling, UUID>,
                                          InsertUpdateRepository<FaktaFeilutbetaling>