package no.nav.tilbakekreving.repository

import no.nav.tilbakekreving.entities.FristUtsettelseEntity
import no.nav.tilbakekreving.entity.FristUtsettelseEntityMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class NyUtsettUttalelseRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun hentUtsettUttalelseFrist(behandlingId: UUID): FristUtsettelseEntity? {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_utsett_uttalelse WHERE behandling_ref=?",
            behandlingId,
        ) { resultSet, _ ->
            FristUtsettelseEntityMapper.map(resultSet)
        }.singleOrNull()
    }

    fun lagre(fristUtsettelseEntity: FristUtsettelseEntity?, behandlingId: UUID) {
        jdbcTemplate.update("DELETE FROM tilbakekreving_utsett_uttalelse WHERE behandling_ref = ?", behandlingId)
        fristUtsettelseEntity?.let { fristUtsettelse ->
            FristUtsettelseEntityMapper.upsertQuery(jdbcTemplate, fristUtsettelse)
        }
    }
}
