package no.nav.tilbakekreving.kravgrunnlag

import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.time.LocalDateTime

@Repository
class KravgrunnlagBufferRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    private val log = TracedLogger.getLogger<KravgrunnlagBufferRepository>()

    fun lagre(entity: Entity) {
        jdbcTemplate.update("INSERT INTO kravgrunnlag_buffer(kravgrunnlag_id, kravgrunnlag, fagsystem_id) VALUES (?, ?, ?);", entity.kravgrunnlagId, entity.kravgrunnlag, entity.fagsystemId)
    }

    fun hent(): List<Entity> {
        return jdbcTemplate.query("SELECT * FROM kravgrunnlag_buffer;", Mapper)
    }

    @Transactional
    fun konsumerKravgrunnlag(callback: (Entity) -> Unit) {
        val kravgrunnlag = jdbcTemplate.query("SELECT * FROM kravgrunnlag_buffer WHERE lest=false ORDER BY mottatt FOR UPDATE LIMIT 5;", Mapper)
        kravgrunnlag.forEach {
            try {
                callback(it)
                jdbcTemplate.update("UPDATE kravgrunnlag_buffer SET lest=true WHERE kravgrunnlag_id=?;", it.kravgrunnlagId)
            } catch (e: Exception) {
                log.medContext(SecureLog.Context.utenBehandling(it.fagsystemId)) {
                    error("Feilet under konsumering av kravgrunnlag, kravgrunnlagId={}", it.kravgrunnlagId, e)
                }
            }
        }
    }

    fun hentKravgrunnlag(kravgrunnlagId: String): List<Entity> {
        return jdbcTemplate.query("SELECT * FROM kravgrunnlag_buffer WHERE kravgrunnlag_id=?;", Mapper, kravgrunnlagId)
    }

    fun hentForFagsystemId(fagsystemId: String): List<Lagret> {
        return jdbcTemplate.query("SELECT * FROM kravgrunnlag_buffer WHERE fagsystem_id=? ORDER BY mottatt;", { rs, rowNum ->
            Lagret(
                entity = Mapper.mapRow(rs, rowNum),
                lest = rs.getBoolean("lest"),
                mottatt = rs.getTimestamp("mottatt").toLocalDateTime(),
            )
        }, fagsystemId)
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
            )
        }
    }

    data class Entity(
        val kravgrunnlag: String,
        val kravgrunnlagId: String,
        val fagsystemId: String,
    )

    data class Lagret(
        val entity: Entity,
        val lest: Boolean,
        val mottatt: LocalDateTime,
    )
}
