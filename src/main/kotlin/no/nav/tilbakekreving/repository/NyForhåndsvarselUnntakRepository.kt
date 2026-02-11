package no.nav.tilbakekreving.repository

import no.nav.tilbakekreving.entities.ForhåndsvarselUnntakEntity
import no.nav.tilbakekreving.entity.ForhåndsvarselUnntakEntityMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class NyForhåndsvarselUnntakRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun hentForhåndsvarselUnntak(behandlingRef: UUID): ForhåndsvarselUnntakEntity? {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_forhåndsvarsel_unntak WHERE behandling_ref=?",
            behandlingRef,
        ) { resultSet, _ ->
            ForhåndsvarselUnntakEntityMapper.map(resultSet)
        }.singleOrNull()
    }

    fun lagre(forhåndsvarselUnntakEntity: ForhåndsvarselUnntakEntity?, behandlingId: UUID) {
        jdbcTemplate.update("DELETE FROM tilbakekreving_brukeruttalelse WHERE behandling_ref=?;", behandlingId)
        jdbcTemplate.update("DELETE FROM tilbakekreving_forhåndsvarsel_unntak WHERE behandling_ref=?", behandlingId)
        forhåndsvarselUnntakEntity?.let { ForhåndsvarselUnntakEntityMapper.upsertQuery(jdbcTemplate, it) }
    }
}
