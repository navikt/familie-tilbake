package no.nav.tilbakekreving.repository

import no.nav.tilbakekreving.entities.ForeldelseperiodeEntity
import no.nav.tilbakekreving.entities.ForeldelsesstegEntity
import no.nav.tilbakekreving.entities.ForskjellEntity
import no.nav.tilbakekreving.entity.Entity.Companion.get
import no.nav.tilbakekreving.entity.ForeldelsesvurderingEntityMapper
import no.nav.tilbakekreving.entity.ForskjellEntityMapper
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
            val periodeId = resultSet[ForeldelsesvurderingEntityMapper.VurdertPeriode.id]
            ForeldelsesvurderingEntityMapper.VurdertPeriode.map(
                resultSet,
                hentEndringIKravgrunnlag(periodeId),
            )
        }
    }

    fun lagre(foreldelsestegEntity: ForeldelsesstegEntity) {
        ForeldelsesvurderingEntityMapper.upsertQuery(jdbcTemplate, foreldelsestegEntity)
        jdbcTemplate.update("DELETE FROM tilbakekreving_foreldelsesvurdering_periode WHERE foreldelsesvurdering_ref=?;", foreldelsestegEntity.id)
        for (vurdertPeriode in foreldelsestegEntity.vurdertePerioder) {
            ForeldelsesvurderingEntityMapper.VurdertPeriode.upsertQuery(jdbcTemplate, vurdertPeriode)
            val endringIKravgrunnlag = vurdertPeriode.endringIKravgrunnlag
            if (endringIKravgrunnlag != null) {
                ForskjellEntityMapper.upsertQuery(jdbcTemplate, endringIKravgrunnlag)
            }
        }
    }

    private fun hentEndringIKravgrunnlag(foreldelsesvurderingPeriodeId: UUID): ForskjellEntity? {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_kravgrunnlag_forskjell WHERE foreldelsesvurdering_periode_ref=?",
            foreldelsesvurderingPeriodeId,
        ) { resultSet, _ ->
            ForskjellEntityMapper.map(resultSet)
        }.singleOrNull()
    }
}
