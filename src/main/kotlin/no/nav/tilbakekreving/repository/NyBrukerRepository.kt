package no.nav.tilbakekreving.repository

import no.nav.tilbakekreving.entities.BrukerEntity
import no.nav.tilbakekreving.entity.BrukerEntityMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository

@Repository
class NyBrukerRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun hentBruker(tilbakekrevingId: String): BrukerEntity? {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_bruker WHERE tilbakekreving_ref = ?",
            tilbakekrevingId,
        ) { resultSet, _ ->
            BrukerEntityMapper.map(resultSet)
        }.singleOrNull()
    }

    fun lagre(brukerEntity: BrukerEntity) {
        jdbcTemplate.update("DELETE FROM tilbakekreving_bruker WHERE tilbakekreving_ref=?;", brukerEntity.tilbakekrevingRef)
        BrukerEntityMapper.insertQuery(jdbcTemplate, brukerEntity)
    }
}
