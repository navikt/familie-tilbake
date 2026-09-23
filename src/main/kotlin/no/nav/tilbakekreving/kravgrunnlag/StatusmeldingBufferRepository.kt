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
            "SELECT COUNT(1) as count FROM statusmelding_buffer WHERE fagsystem_id=? AND status='AVSL';",
            fagsystemId,
        ) { rs, _ ->
            rs.getInt("count") > 0
        }.singleOrNull() ?: false
    }

    fun erSperret(fagsystemId: String): Boolean {
        return jdbcTemplate.query(
            """
            SELECT EXISTS(
                SELECT 1
                FROM statusmelding_buffer sm
                WHERE sm.fagsystem_id = ?
                  AND sm.status = 'SPER'
                  AND sm.mottatt > '2026-09-02'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM kravgrunnlag_buffer kg
                      WHERE kg.fagsystem_id = sm.fagsystem_id
                        AND kg.mottatt > sm.mottatt
                  )
            ) AS is_sperret;
            """.trimIndent(),
            fagsystemId,
        ) { rs, _ ->
            rs.getBoolean("is_sperret")
        }.singleOrNull() ?: false
    }

    data class Entity(
        val statusmelding: String,
        val fagsystemId: String,
        val vedtakId: String,
        val status: String,
    )
}
