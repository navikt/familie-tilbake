package no.nav.familie.tilbake.kravgrunnlag

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.kravgrunnlag.domain.Kravgrunnlag431
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.util.UUID

@Repository
@Transactional
interface KravgrunnlagRepository :
    RepositoryInterface<Kravgrunnlag431, UUID>,
    InsertUpdateRepository<Kravgrunnlag431> {
    fun findByBehandlingIdAndAktivIsTrue(behandlingId: UUID): Kravgrunnlag431

    fun findByBehandlingIdAndAktivIsTrueAndSperretTrue(behandlingId: UUID): Kravgrunnlag431

    fun existsByBehandlingIdAndAktivTrue(behandlingId: UUID): Boolean

    fun existsByBehandlingIdAndAktivTrueAndSperretFalse(behandlingId: UUID): Boolean

    fun existsByBehandlingIdAndAktivTrueAndSperretTrue(behandlingId: UUID): Boolean

    fun findByBehandlingId(behandlingId: UUID): List<Kravgrunnlag431>

    @Query("SELECT DISTINCT behandling_id FROM kravgrunnlag431 WHERE vedtak_id = CAST(:vedtakId AS BIGINT) AND behandling_id IS NOT NULL")
    fun findBehandlingIdsByVedtakId(vedtakId: BigInteger): List<UUID>

    fun findByEksternKravgrunnlagIdAndAktivIsTrue(eksternKravgrunnlagId: BigInteger): Kravgrunnlag431?
}
