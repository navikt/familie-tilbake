package no.nav.tilbakekreving.kravgrunnlag

import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagBufferRepository.Entity
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagBufferRepository.Mapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.util.UUID
import kotlin.collections.forEach

@Repository
class StatusmeldingBufferRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    private val log = TracedLogger.getLogger<StatusmeldingBufferRepository>()

    fun lagre(entity: Entity) {
        jdbcTemplate.update(
            "INSERT INTO statusmelding_buffer(statusmelding, fagsystem_id, vedtak_id, status) VALUES (?, ?, ?, ?);",
            entity.statusmelding,
            entity.fagsystemId,
            entity.vedtakId,
            entity.status,
        )
    }

    @Transactional
    fun konsumerStatusmelding(callback: (Entity) -> Unit) {
        val statusmelding = jdbcTemplate.query("SELECT * FROM statusmelding_buffer WHERE lest=false ORDER BY mottatt FOR UPDATE LIMIT 5;", Mapper)
        statusmelding.forEach {
            try {
                callback(it)
                jdbcTemplate.update("UPDATE statusmelding_buffer SET lest=true WHERE statusmelding_id=?;", it.statusmeldingId)
            } catch (e: Exception) {
                log.medContext(SecureLog.Context.utenBehandling(it.fagsystemId)) {
                    error("Feilet under konsumering av statusmelding, statusmeldingId={}", it.statusmeldingId, e)
                }
            }
        }
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

    object Mapper : RowMapper<StatusmeldingBufferRepository.Entity> {
        override fun mapRow(
            rs: ResultSet,
            rowNum: Int,
        ): StatusmeldingBufferRepository.Entity {
            return Entity(
                UUID.fromString(rs.getString("statusmelding_id")),
                rs.getString("statusmelding"),
                rs.getString("fagsystem_id"),
                rs.getString("vedtak_id"),
                rs.getString("status"),
            )
        }
    }

    data class Entity(
        val statusmeldingId: UUID,
        val statusmelding: String,
        val fagsystemId: String,
        val vedtakId: String,
        val status: String,
    )
}
