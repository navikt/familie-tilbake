package no.nav.tilbakekreving.kravgrunnlag

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import java.sql.ResultSet

@Repository
class KravgrunnlagBufferRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun lagre(entity: Entity) {
        jdbcTemplate.update("INSERT INTO kravgrunnlag_buffer(kravgrunnlag_id, kravgrunnlag) VALUES (?, ?);", entity.kravgrunnlagId, entity.kravgrunnlag)
    }

    fun hent(): List<Entity> {
        return jdbcTemplate.query("SELECT * FROM kravgrunnlag_buffer;", Mapper)
    }

    fun hentUlesteKravgrunnlag(): List<Entity> {
        return jdbcTemplate.query("SELECT * FROM kravgrunnlag_buffer WHERE lest=false;", Mapper)
    }

    fun markerLest(kravgrunnlagId: String) {
        jdbcTemplate.update("UPDATE kravgrunnlag_buffer SET lest=true WHERE kravgrunnlag_id=?;", kravgrunnlagId)
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
            )
        }
    }

    data class Entity(
        val kravgrunnlag: String,
        val kravgrunnlagId: String,
    )
}
