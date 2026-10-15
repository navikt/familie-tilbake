package no.nav.tilbakekreving.kravgrunnlag

import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
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
                jdbcTemplate.update("UPDATE statusmelding_buffer SET lest=true WHERE id=?;", it.id)
            } catch (e: Exception) {
                log.medContext(SecureLog.Context.utenBehandling(it.fagsystemId)) {
                    error("Feilet under konsumering av statusmelding, statusmeldingId={}", it.id, e)
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

    object Mapper : RowMapper<StatusmeldingBufferRepository.Entity> {
        override fun mapRow(
            rs: ResultSet,
            rowNum: Int,
        ): StatusmeldingBufferRepository.Entity {
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
