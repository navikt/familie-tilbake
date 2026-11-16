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

@Repository
class KravgrunnlagBufferRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    private val log = TracedLogger.getLogger<KravgrunnlagBufferRepository>()

    fun lagre(entity: Entity) {
        jdbcTemplate.update(
            "INSERT INTO kravgrunnlag_buffer(kravgrunnlag_id, kravgrunnlag, fagsystem_id, mottatt) VALUES (?, ?, ?, ?);",
            entity.kravgrunnlagId,
            entity.kravgrunnlag,
            entity.fagsystemId,
            entity.mottatt,
        )
    }

    fun hent(): List<Entity> {
        return jdbcTemplate.query("SELECT * FROM kravgrunnlag_buffer;", Mapper)
    }

    @Transactional
    fun konsumerKravgrunnlag(callback: (Entity) -> Unit) {
        val kravgrunnlag = jdbcTemplate.query("SELECT * FROM kravgrunnlag_buffer WHERE lest=false ORDER BY mottatt FOR UPDATE LIMIT 5;") { rs, rowNum ->
            rs.getObject("id", UUID::class.java) to Mapper.mapRow(rs, rowNum)
        }
        kravgrunnlag.forEach { (id, entity) ->
            try {
                callback(entity)
                jdbcTemplate.update("UPDATE kravgrunnlag_buffer SET lest=true WHERE id=?;", id)
            } catch (e: Exception) {
                log.medContext(SecureLog.Context.utenBehandling(entity.fagsystemId)) {
                    error("Feilet under konsumering av kravgrunnlag, kravgrunnlagId={}", entity.kravgrunnlagId, e)
                }
            }
        }
    }

    fun hentKravgrunnlag(kravgrunnlagId: String): List<Entity> {
        return jdbcTemplate.query("SELECT * FROM kravgrunnlag_buffer WHERE kravgrunnlag_id=?;", Mapper, kravgrunnlagId)
    }

    object Mapper : RowMapper<Entity> {
        override fun mapRow(
            rs: ResultSet,
            rowNum: Int,
        ): Entity {
            return Entity(
                rs.getString("kravgrunnlag"),
                rs.getString("kravgrunnlag_id"),
                rs.getString("fagsystem_id"),
                rs.getTimestamp("mottatt").toLocalDateTime(),
            )
        }
    }

    data class Entity(
        val kravgrunnlag: String,
        val kravgrunnlagId: String,
        val fagsystemId: String,
        val mottatt: LocalDateTime,
    )
}
