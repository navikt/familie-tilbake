package no.nav.tilbakekreving.kravgrunnlag

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository

@Repository
class StatusmeldingBufferRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun lagre(entity: Entity) {
        jdbcTemplate.update(
            "INSERT INTO statusmelding_buffer(statusmelding, fagsystem_id, vedtak_id, status) VALUES (?, ?, ?, ?);",
            entity.statusmelding,
            entity.fagsystemId,
            entity.vedtakId,
            entity.status,
        )
    }

    fun erAnnullert(fagsystemId: String): Boolean {
        return jdbcTemplate.query(
            "SELECT COUNT(1) as count FROM statusmelding_buffer WHERE fagsystem_id=? AND status IN ('ANNU', 'ANOM');",
            fagsystemId,
        ) { rs, _ ->
            rs.getInt("count") > 0
        }.singleOrNull() ?: false
    }

    data class Entity(
        val statusmelding: String,
        val fagsystemId: String,
        val vedtakId: String,
        val status: String,
    )
}
