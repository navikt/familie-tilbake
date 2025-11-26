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
    fun hentUtsettUttalelseFrist(behandlingId: UUID): List<FristUtsettelseEntity> {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_utsett_uttalelse WHERE behandling_ref=?",
            behandlingId,
        ) { resultSet, _ ->
            FristUtsettelseEntityMapper.map(resultSet)
        }
    }

    fun lagre(fristUtsettelseEntity: List<FristUtsettelseEntity>) {
        fristUtsettelseEntity.forEach { fristUtsettelse ->
            FristUtsettelseEntityMapper.upsertQuery(jdbcTemplate, fristUtsettelse)
        }
    }
}
