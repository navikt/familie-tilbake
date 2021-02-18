package no.nav.familie.tilbake.repository.tbd

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.domain.tbd.Kravgrunnlag431
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface Kravgrunnlag431Repository : RepositoryInterface<Kravgrunnlag431, UUID>,
                                      InsertUpdateRepository<Kravgrunnlag431> {

    // language=PostgreSQL
    @Query("""SELECT k.* FROM kravgrunnlag431 k
              JOIN  gruppering_krav_grunnlag gkg
              ON gkg.kravgrunnlag431_id = k.id  
              AND gkg.aktiv = TRUE 
              WHERE gkg.behandling_id = :behandlingId""")
    fun findForAgregate(behandlingId: UUID): Kravgrunnlag431?
}
