package no.nav.tilbakekreving

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.tilbakekreving.entities.TilbakekrevingEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class TilbakekrevingRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(KotlinModule.Builder().build())
        .registerModule(JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)

    @Transactional
    fun lagre(
        tilbakekrevingId: UUID,
        behandlingId: UUID,
        tilbakekrevingEntity: TilbakekrevingEntity,
    ) {
        jdbcTemplate.update(
            """
        INSERT INTO tilbakekreving_behandling(tilbakekreving_id, behandling_id)
        VALUES (?, ?)
        ON CONFLICT (tilbakekreving_id, behandling_id) DO NOTHING
            """.trimIndent(),
            tilbakekrevingId,
            behandlingId,
        )

        lagreTilstand(tilbakekrevingEntity)
    }

    fun hentTilbakekreving(behandlingId: UUID): TilbakekrevingEntity? {
        val tilbakekrevingId: UUID? = jdbcTemplate.query(
            "SELECT tilbakekreving_id FROM tilbakekreving_behandling WHERE behandling_id = ?",
            arrayOf(behandlingId),
        ) { rs, _ -> rs.getObject("tilbakekreving_id", UUID::class.java) }
            .firstOrNull()

        return tilbakekrevingId?.let { hentTilbakekrevingMedTilbakekrevingId(it) }
    }

    fun hentAlleTilbakekrevinger(): List<TilbakekrevingEntity>? {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_snapshot",
        ) { rs, _ ->
            val jsonText = rs.getString("snapshot")
            objectMapper.readValue(jsonText, TilbakekrevingEntity::class.java)
        }.toList()
    }

    @Transactional
    fun lagreTilstand(snapshot: TilbakekrevingEntity) {
        val jsonText = objectMapper.writeValueAsString(snapshot)
        jdbcTemplate.update(
            """
            INSERT INTO tilbakekreving_snapshot (id, snapshot) 
            VALUES (?, to_jsonb(?::json)) 
            ON CONFLICT (id) DO UPDATE SET snapshot = excluded.snapshot
            """.trimIndent(),
            snapshot.id,
            jsonText,
        )
    }

    fun hentTilbakekrevingMedTilbakekrevingId(id: UUID): TilbakekrevingEntity? {
        return jdbcTemplate.query(
            "SELECT snapshot FROM tilbakekreving_snapshot WHERE id = ?",
            arrayOf(id),
        ) { rs, _ ->
            val jsonText = rs.getString("snapshot")
            objectMapper.readValue(jsonText, TilbakekrevingEntity::class.java)
        }.firstOrNull()
    }
}
