package no.nav.tilbakekreving

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.sync.RedisCommands
import no.nav.tilbakekreving.entities.TilbakekrevingEntity
import java.util.UUID

object ValkeyClient {
    private val redisClient: RedisClient = RedisClient.create("redis://localhost:6379")
    private val connection: StatefulRedisConnection<String, String> = redisClient.connect()
    private val commands: RedisCommands<String, String> = connection.sync()
    private val objectMapper = jacksonObjectMapper()

    fun lagreTilstand(
        tilbakekrevingId: UUID,
        tilbakekrevingEntity: TilbakekrevingEntity,
    ) {
        val json = objectMapper.writeValueAsString(tilbakekrevingEntity)
        commands.set("tilbakekreving:$tilbakekrevingId", json)
    }

    fun henteTilstand(tilbakekrevingId: UUID): TilbakekrevingEntity? {
        val json = commands.get("tilbakekreving:$tilbakekrevingId") ?: return null
        return objectMapper.readValue(json)
    }

    fun fjerneTilstand(tilbakekrevingId: String) {
        commands.del("tilbakekreving:$tilbakekrevingId")
    }
}
