package no.nav.tilbakekreving.repository

import no.nav.tilbakekreving.entities.UttalelsesfristEntity
import no.nav.tilbakekreving.entity.FristUtsettelseEntityMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class NyUtsettUttalelseRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun hentUtsettUttalelseFrist(behandlingId: UUID): UttalelsesfristEntity? {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_uttalelsesfrist WHERE behandling_ref=?",
            behandlingId,
        ) { resultSet, _ ->
            FristUtsettelseEntityMapper.map(resultSet)
        }.singleOrNull()
    }

    fun lagre(uttalelsesfristEntity: UttalelsesfristEntity?, behandlingId: UUID) {
        jdbcTemplate.update("DELETE FROM tilbakekreving_uttalelsesfrist WHERE behandling_ref = ?", behandlingId)
        uttalelsesfristEntity?.let { fristUtsettelse ->
            FristUtsettelseEntityMapper.upsertQuery(jdbcTemplate, fristUtsettelse)
        }
    }
}
