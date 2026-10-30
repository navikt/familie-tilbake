package no.nav.tilbakekreving.repository

import no.nav.tilbakekreving.entities.TilbakekrevingRevurderingEntity
import no.nav.tilbakekreving.entity.TilbakekrevingRevurderingEntityMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class NyRevurderingRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun hentTilbakekrevingRevurdering(behandlingRef: UUID): TilbakekrevingRevurderingEntity? {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_revurdering WHERE behandling_ref = ?",
            behandlingRef,
        ) { resultSet, _ ->
            TilbakekrevingRevurderingEntityMapper.map(resultSet)
        }.singleOrNull()
    }

    fun lagre(revurdering: TilbakekrevingRevurderingEntity) {
        jdbcTemplate.update("DELETE FROM tilbakekreving_revurdering WHERE behandling_ref=?;", revurdering.behandlingRef)
        TilbakekrevingRevurderingEntityMapper.insertQuery(jdbcTemplate, revurdering)
    }
}
