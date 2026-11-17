package no.nav.tilbakekreving.repository

import no.nav.tilbakekreving.entities.ForeslåVedtakStegEntity
import no.nav.tilbakekreving.entity.ForeslåVedtakEntityMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class NyForeslåVedtakRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun hentForeslåttVedtak(
        behandlingId: UUID,
    ): ForeslåVedtakStegEntity {
        return jdbcTemplate.timedQuery(
            "tilbakekreving_foreslåvedtak",
            "SELECT * FROM tilbakekreving_foreslåvedtak WHERE behandling_ref = ?",
            behandlingId,
        ) { resultSet, _ ->
            ForeslåVedtakEntityMapper.map(
                resultSet,
            )
        }.single()
    }

    fun lagre(foreslåVedtakStegEntity: ForeslåVedtakStegEntity) {
        ForeslåVedtakEntityMapper.upsertQuery(jdbcTemplate, foreslåVedtakStegEntity)
    }
}
