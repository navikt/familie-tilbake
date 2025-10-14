package no.nav.tilbakekreving.repository

import no.nav.tilbakekreving.entities.ForeldelseperiodeEntity
import no.nav.tilbakekreving.entities.ForeldelsesstegEntity
import no.nav.tilbakekreving.entity.Entity.Companion.get
import no.nav.tilbakekreving.entity.ForeldelsesvurderingEntityMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class NyForeldelseRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun hentForeldelsesvurdering(behandlingId: UUID): ForeldelsesstegEntity {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_foreldelsesvurdering WHERE behandling_ref=?",
            behandlingId,
        ) { resultSet, _ ->
            val vurderingId = resultSet[ForeldelsesvurderingEntityMapper.id]
            ForeldelsesvurderingEntityMapper.map(resultSet, hentPerioder(vurderingId))
        }.single()
    }

    private fun hentPerioder(vurderingId: UUID): List<ForeldelseperiodeEntity> {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_foreldelsesvurdering_periode WHERE foreldelsesvurdering_ref=?",
            vurderingId,
        ) { resultSet, _ ->
            ForeldelsesvurderingEntityMapper.VurdertPeriode.map(resultSet)
        }
    }

    fun lagre(foreldelsestegEntity: ForeldelsesstegEntity) {
        ForeldelsesvurderingEntityMapper.insertQuery(jdbcTemplate, foreldelsestegEntity)
        jdbcTemplate.update("DELETE FROM tilbakekreving_foreldelsesvurdering_periode WHERE foreldelsesvurdering_ref=?;", foreldelsestegEntity.id)
        for (vurdertPeriode in foreldelsestegEntity.vurdertePerioder) {
            ForeldelsesvurderingEntityMapper.VurdertPeriode.upsertQuery(jdbcTemplate, vurdertPeriode)
        }
    }
}
