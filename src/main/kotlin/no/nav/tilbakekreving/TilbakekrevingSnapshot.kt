package no.nav.tilbakekreving

import kotlinx.serialization.json.Json
import no.nav.tilbakekreving.entities.TilbakekrevingEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class TilbakekrevingSnapshot(
    private val jdbcTemplate: JdbcTemplate,
) {
    private val json = Json { ignoreUnknownKeys = true }

    @Transactional
    fun lagreTilstand(snapshot: TilbakekrevingEntity) {
        val jsonText = json.encodeToString(snapshot)
        jdbcTemplate.update(
            """
            INSERT INTO tilbakekreving_snapshot (id, snapshot) 
            VALUES (?, to_jsonb(?::json)) 
            ON CONFLICT (id) DO UPDATE SET snapshot = excluded.snapshot
            """.trimIndent(),
            UUID.fromString(snapshot.id),
            jsonText,
        )
    }

    fun henteTilstand(id: UUID): TilbakekrevingEntity? {
        return jdbcTemplate.query(
            "SELECT snapshot FROM tilbakekreving_snapshot WHERE id = ?",
            arrayOf(id),
        ) { rs, _ ->
            val jsonText = rs.getString("snapshot")
            json.decodeFromString<TilbakekrevingEntity>(jsonText)
        }.firstOrNull()
    }

    fun testForTull(): UUID? {
        return jdbcTemplate.queryForObject(
            "SELECT id FROM behandling LIMIT 1",
            UUID::class.java,
        )
    }
}
