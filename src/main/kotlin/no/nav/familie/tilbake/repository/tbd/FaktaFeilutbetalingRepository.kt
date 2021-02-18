package no.nav.familie.tilbake.repository.tbd

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.domain.tbd.FaktaFeilutbetaling
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
@Transactional
interface FaktaFeilutbetalingRepository : RepositoryInterface<FaktaFeilutbetaling, UUID>,
                                          InsertUpdateRepository<FaktaFeilutbetaling> {

    // language=PostgreSQL
    @Query("""SELECT f.* FROM fakta_feilutbetaling f
              JOIN  gruppering_fakta_feilutbetaling gff
              ON gff.fakta_feilutbetaling_id = f.id  
              AND gff.aktiv = TRUE 
              WHERE gff.behandling_id = :behandlingId""")
    fun findByBehandlingId(behandlingId: UUID): FaktaFeilutbetaling?
}
