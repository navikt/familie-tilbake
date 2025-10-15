package no.nav.tilbakekreving.repository

import no.nav.tilbakekreving.entities.FaktastegEntity
import no.nav.tilbakekreving.entity.Entity.Companion.get
import no.nav.tilbakekreving.entity.FaktavurderingEntityMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class NyFaktavurderingRepository(private val jdbcTemplate: JdbcTemplate) {
    fun hentFaktavurdering(behandlingId: UUID): FaktastegEntity {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_faktavurdering WHERE behandling_ref=?",
            behandlingId,
        ) { resultSet, _ ->
            val id = resultSet[FaktavurderingEntityMapper.id]
            FaktavurderingEntityMapper.map(resultSet, hentPerioder(id))
        }.single()
    }

    fun lagre(faktavurdering: FaktastegEntity) {
        FaktavurderingEntityMapper.upsertQuery(jdbcTemplate, faktavurdering)
        lagrePerioder(faktavurdering.id, faktavurdering.perioder)
    }

    private fun hentPerioder(faktavurderingRef: UUID): List<FaktastegEntity.FaktaPeriodeEntity> {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_faktavurdering_periode WHERE faktavurdering_ref=?",
            faktavurderingRef,
        ) { resultSet, _ ->
            FaktavurderingEntityMapper.FaktavurderingPeriodeEntityMapper.map(resultSet)
        }
    }

    private fun lagrePerioder(faktavurderingRef: UUID, perioder: List<FaktastegEntity.FaktaPeriodeEntity>) {
        jdbcTemplate.update("DELETE FROM tilbakekreving_faktavurdering_periode WHERE faktavurdering_ref=?", faktavurderingRef)
        for (periode in perioder) {
            FaktavurderingEntityMapper.FaktavurderingPeriodeEntityMapper.upsertQuery(jdbcTemplate, periode)
        }
    }
}
