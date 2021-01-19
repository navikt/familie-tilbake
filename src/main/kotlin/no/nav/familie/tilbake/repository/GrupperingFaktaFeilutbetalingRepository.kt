package no.nav.familie.tilbake.repository

import no.nav.familie.tilbake.domain.GrupperingFaktaFeilutbetaling
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface GrupperingFaktaFeilutbetalingRepository : RepositoryInterface<GrupperingFaktaFeilutbetaling, UUID>,
                                                    InsertUpdateRepository<GrupperingFaktaFeilutbetaling>