package no.nav.tilbakekreving.kravgrunnlag

import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.time.LocalDateTime
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
    fun konsumerStatusmelding(callback: (entity: Entity, mottatt: LocalDateTime) -> Unit) {
        val statusmelding = jdbcTemplate.query("SELECT * FROM statusmelding_buffer WHERE lest=false ORDER BY mottatt FOR UPDATE LIMIT 5;") { rs, rowNum ->
            Mapper.mapRow(rs, rowNum) to rs.getTimestamp("mottatt").toLocalDateTime()
        }
        statusmelding.forEach { (entity, mottatt) ->
            try {
                callback(entity, mottatt)
                jdbcTemplate.update("UPDATE statusmelding_buffer SET lest=true WHERE id=?;", entity.id)
            } catch (e: Exception) {
                log.medContext(SecureLog.Context.utenBehandling(entity.fagsystemId)) {
                    error("Feilet under konsumering av statusmelding, statusmeldingId={}", entity.id, e)
                }
            }
        }
    }

    object Mapper : RowMapper<Entity> {
        override fun mapRow(
            rs: ResultSet,
            rowNum: Int,
        ): Entity {
            return Entity(
                UUID.fromString(rs.getString("id")),
                rs.getString("statusmelding"),
                rs.getString("fagsystem_id"),
                rs.getString("vedtak_id"),
                rs.getString("status"),
            )
        }
    }

    data class Entity(
        val id: UUID,
        val statusmelding: String,
        val fagsystemId: String,
        val vedtakId: String,
        val status: String,
    )
}
