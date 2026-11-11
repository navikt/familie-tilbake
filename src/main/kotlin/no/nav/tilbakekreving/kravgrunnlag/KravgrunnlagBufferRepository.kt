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

    fun hentForFagsystemId(fagsystemId: String): List<Lagret> {
        return jdbcTemplate.query("SELECT * FROM kravgrunnlag_buffer WHERE fagsystem_id=? ORDER BY mottatt;", { rs, rowNum ->
            Lagret(
                entity = Mapper.mapRow(rs, rowNum),
                lest = rs.getBoolean("lest"),
            )
        }, fagsystemId)
    }

    fun finnKravgrunnlagUtenSak(): List<KravgrunnlagUtenSak> {
        return jdbcTemplate.query(
            """SELECT kb.fagsystem_id, kb.vedtak_id, kb.kontrollfelt
FROM (SELECT fagsystem_id,
             mottatt,
             (xpath(
                     '/urn:detaljertKravgrunnlagMelding/urn:detaljertKravgrunnlag/urn:vedtakId/text()',
                     kravgrunnlag::xml,
                     ARRAY [ARRAY ['urn', 'urn:no:nav:tilbakekreving:kravgrunnlag:detalj:v1']]
              ))[1]::text AS vedtak_id,
             (xpath(
                     '/urn:detaljertKravgrunnlagMelding/urn:detaljertKravgrunnlag/urn:kontrollfelt/text()',
                     kravgrunnlag::xml,
                     ARRAY [ARRAY ['urn', 'urn:no:nav:tilbakekreving:kravgrunnlag:detalj:v1']]
              ))[1]::text AS kontrollfelt
      FROM kravgrunnlag_buffer) kb
WHERE NOT EXISTS (SELECT 1 FROM tilbakekreving_kravgrunnlag tk WHERE tk.kontrollfelt = kb.kontrollfelt)
ORDER BY kb.mottatt;""",
        ) { rs, _ ->
            KravgrunnlagUtenSak(
                fagsystemId = rs.getString("fagsystem_id"),
                vedtakId = rs.getString("vedtak_id"),
                kontrollfelt = rs.getString("kontrollfelt"),
            )
        }
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

    data class KravgrunnlagUtenSak(
        val fagsystemId: String,
        val vedtakId: String?,
        val kontrollfelt: String?,
    )

    data class Lagret(
        val entity: Entity,
        val lest: Boolean,
    )
}
