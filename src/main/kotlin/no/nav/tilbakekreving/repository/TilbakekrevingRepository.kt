package no.nav.tilbakekreving.repository

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.tilbakekreving.entities.TilbakekrevingEntity
import no.nav.tilbakekreving.entity.Entity.Companion.get
import no.nav.tilbakekreving.entity.FieldConverter
import no.nav.tilbakekreving.entity.TilbakekrevingEntityMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.util.UUID

@Repository
class TilbakekrevingRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val behandlingRepository: NyBehandlingRepository,
    private val kravgrunnlagRepository: NyKravgrunnlagRepository,
    private val eksternFagsakRepository: NyEksternFagsakRepository,
) {
    private val objectMapper = jacksonObjectMapper()
        .registerModule(KotlinModule.Builder().build())
        .registerModule(JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)

    fun nesteId(): String {
        return jdbcTemplate.query("SELECT nextval('tilbakekreving_id')") { rs, _ ->
            FieldConverter.NumericId.convert(rs, "nextval")
        }.single()
    }

    private fun mergeMedJson(
        resultSet: ResultSet,
    ): TilbakekrevingEntity {
        val id = resultSet[TilbakekrevingEntityMapper.id]
        val json = jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_snapshot WHERE id=?;",
            id,
        ) { rs, _ ->
            val jsonText = rs.getString("snapshot")
            objectMapper.readValue(jsonText, TilbakekrevingEntity::class.java)
        }.single()

        return TilbakekrevingEntityMapper.map(
            resultSet = resultSet,
            eksternFagsak = eksternFagsakRepository.hentEksternFagsak(id),
            behandlingHistorikk = behandlingRepository.hentBehandlinger(id, json.behandlingHistorikkEntities),
            kravgrunnlagHistorikk = kravgrunnlagRepository.hentKravgrunnlag(id),
            brevHistorikk = json.brevHistorikkEntities,
            bruker = json.bruker,
        )
    }

    fun hentTilbakekreving(behandlingId: UUID): TilbakekrevingEntity? {
        return hentAlleTilbakekrevinger()
            ?.firstOrNull { tilbakekreving ->
                tilbakekreving.behandlingHistorikkEntities
                    .any { it.id == behandlingId }
            }
    }

    fun hentAlleTilbakekrevinger(): List<TilbakekrevingEntity>? {
        return jdbcTemplate.query("SELECT * FROM tilbakekreving") { rs, _ ->
            mergeMedJson(rs)
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun opprett(
        tilbakekrevingEntity: TilbakekrevingEntity,
    ): String {
        TilbakekrevingEntityMapper.insertQuery(
            jdbcTemplate,
            tilbakekrevingEntity,
        )
        lagreUnderobjekter(tilbakekrevingEntity)
        jdbcTemplate.update(
            "INSERT INTO tilbakekreving_snapshot(id, snapshot) VALUES (?, ?);",
            tilbakekrevingEntity.id,
            objectMapper.writeValueAsString(tilbakekrevingEntity),
        )

        return tilbakekrevingEntity.id
    }

    // Denne er inline så den blir inlinet i transactions
    private inline fun hentForOppdatering(tilbakekrevingId: String): TilbakekrevingEntity {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving WHERE id=? FOR UPDATE;",
            FieldConverter.NumericId.convert(tilbakekrevingId),
        ) { rs, _ ->
            mergeMedJson(rs)
        }.toList().single()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun hentOgLagreResultat(
        tilbakekrevingId: String,
        callback: (TilbakekrevingEntity) -> TilbakekrevingEntity,
    ) {
        val oppdatertEntity = callback(hentForOppdatering(tilbakekrevingId))
        val jsonText = objectMapper.writeValueAsString(oppdatertEntity)
        TilbakekrevingEntityMapper.updateQuery(
            jdbcTemplate,
            oppdatertEntity,
        )
        lagreUnderobjekter(oppdatertEntity)
        jdbcTemplate.update(
            "UPDATE tilbakekreving_snapshot SET snapshot = to_jsonb(?::json) WHERE id=?;",
            jsonText,
            tilbakekrevingId,
        )
    }

    private fun lagreUnderobjekter(entity: TilbakekrevingEntity) {
        eksternFagsakRepository.lagre(entity.eksternFagsak)
        kravgrunnlagRepository.lagre(entity.kravgrunnlagHistorikkEntities)
        behandlingRepository.lagreBehandlinger(entity.behandlingHistorikkEntities)
    }
}
