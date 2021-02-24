package no.nav.familie.tilbake.faktaomfeilutbetaling

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.FaktaFeilutbetalingsperiode
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface FaktaFeilutbetalingsperiodeRepository : RepositoryInterface<FaktaFeilutbetalingsperiode, UUID>,
                                                  InsertUpdateRepository<FaktaFeilutbetalingsperiode>
