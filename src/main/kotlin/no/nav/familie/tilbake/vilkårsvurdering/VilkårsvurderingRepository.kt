package no.nav.familie.tilbake.vilkårsvurdering

import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.familie.tilbake.vilkårsvurdering.domain.Vilkårsvurdering
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Repository
@Transactional
interface VilkårsvurderingRepository :
    RepositoryInterface<Vilkårsvurdering, UUID>,
    InsertUpdateRepository<Vilkårsvurdering> {
    fun findByBehandlingIdAndAktivIsTrue(behandlingId: UUID): List<Vilkårsvurdering>

    fun findByBehandlingId(behandlingId: UUID): List<Vilkårsvurdering>

    @Query(
        """
        SELECT COUNT(*)
        FROM vilkarsvurderingsperiode
        WHERE vilkarsvurderingsresultat = :vilkarsvurderingsresultat
          AND fom >= :fraOgMed
          AND fom <= :tilOgMed
        """,
    )
    fun countByVilkårsvurderingsresultatAndFomBetween(
        @Param("vilkarsvurderingsresultat") vilkårsvurderingsresultat: String,
        @Param("fraOgMed") fraOgMed: LocalDate,
        @Param("tilOgMed") tilOgMed: LocalDate,
    ): Long
}
