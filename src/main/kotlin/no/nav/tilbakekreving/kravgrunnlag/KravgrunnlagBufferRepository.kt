package no.nav.tilbakekreving.kravgrunnlag

import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.log.TracedLogger
import no.nav.tilbakekreving.UtenforScope
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet

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
        val kravgrunnlag = jdbcTemplate.query("SELECT * FROM kravgrunnlag_buffer WHERE lest=false AND utenfor_scope=false FOR UPDATE LIMIT 5;", Mapper)
        kravgrunnlag.forEach {
            try {
                callback(it)
                jdbcTemplate.update("UPDATE kravgrunnlag_buffer SET lest=true WHERE kravgrunnlag_id=?;", it.kravgrunnlagId)
            } catch (e: ModellFeil.UtenforScopeException) {
                log.medContext(SecureLog.Context.medBehandling(e.sporing.fagsakId, e.sporing.behandlingId)) {
                    error("Kunne ikke konsumere kravgrunnlag. Meldingen er ikke enda støttet", e)
                }
                jdbcTemplate.update("UPDATE kravgrunnlag_buffer SET utenfor_scope=true WHERE kravgrunnlag_id=?;", it.kravgrunnlagId)
            } catch (e: Exception) {
                log.medContext(SecureLog.Context.utenBehandling(it.kravgrunnlagId)) {
                    error("Feilet under konsumering av kravgrunnlag", e)
                }
            }
        }
    }

    fun validerKravgrunnlagInnenforScope(fagsystemId: String, behandlingId: String?) {
        jdbcTemplate.query("SELECT COUNT(1) AS antall FROM kravgrunnlag_buffer WHERE fagsystem_id=?;", fagsystemId) { resultSet, _ ->
            if (resultSet.getInt("antall") > 1) {
                throw ModellFeil.UtenforScopeException(UtenforScope.KravgrunnlagStatusIkkeStøttet, Sporing(fagsystemId, behandlingId ?: "Ukjent"))
            }
        }
    }

    fun hentKravgrunnlag(kravgrunnlagId: String): Entity? {
        return jdbcTemplate.query("SELECT * FROM kravgrunnlag_buffer WHERE kravgrunnlag_id=?;", Mapper, kravgrunnlagId)
            .singleOrNull()
    }

    object Mapper : RowMapper<Entity> {
        override fun mapRow(
            rs: ResultSet,
            rowNum: Int,
        ): Entity? {
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
}
