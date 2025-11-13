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
import no.nav.tilbakekreving.fagsystem.Ytelsestype
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.time.LocalDateTime
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

    fun hentTilbakekreving(strategy: FindTilbakekrevingStrategy): TilbakekrevingEntity? {
        return hentTilbakekrevinger(strategy).firstOrNull()
    }

    fun hentTilbakekrevinger(strategy: FindTilbakekrevingStrategy): List<TilbakekrevingEntity> {
        return strategy.select(jdbcTemplate) { resultSet, index ->
            mergeMedJson(resultSet)
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
    private inline fun hentForOppdatering(strategy: FindTilbakekrevingStrategy): TilbakekrevingEntity? {
        return strategy.selectForUpdate(jdbcTemplate) { rs, _ ->
            mergeMedJson(rs)
        }.toList().firstOrNull()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun hentOgLagreResultat(
        strategy: FindTilbakekrevingStrategy,
        callback: (TilbakekrevingEntity) -> TilbakekrevingEntity,
    ): Boolean {
        val entity = hentForOppdatering(strategy) ?: return false
        val oppdatertEntity = callback(entity)
        val jsonText = objectMapper.writeValueAsString(oppdatertEntity)
        TilbakekrevingEntityMapper.updateQuery(
            jdbcTemplate,
            oppdatertEntity,
        )
        lagreUnderobjekter(oppdatertEntity)
        jdbcTemplate.update(
            "UPDATE tilbakekreving_snapshot SET snapshot = to_jsonb(?::json) WHERE id=?;",
            jsonText,
            oppdatertEntity.id,
        )
        return true
    }

    private fun lagreUnderobjekter(entity: TilbakekrevingEntity) {
        eksternFagsakRepository.lagre(entity.eksternFagsak)
        kravgrunnlagRepository.lagre(entity.kravgrunnlagHistorikkEntities)
        behandlingRepository.lagreBehandlinger(entity.behandlingHistorikkEntities)
    }

    sealed interface FindTilbakekrevingStrategy {
        fun select(jdbcTemplate: JdbcTemplate, mapper: RowMapper<TilbakekrevingEntity>): List<TilbakekrevingEntity>

        fun selectForUpdate(jdbcTemplate: JdbcTemplate, mapper: RowMapper<TilbakekrevingEntity>): List<TilbakekrevingEntity>

        class BehandlingId(val id: UUID) : FindTilbakekrevingStrategy {
            override fun select(jdbcTemplate: JdbcTemplate, mapper: RowMapper<TilbakekrevingEntity>): List<TilbakekrevingEntity> {
                return jdbcTemplate.query("SELECT * FROM tilbakekreving JOIN tilbakekreving_behandling tb ON tilbakekreving.id=tb.tilbakekreving_id WHERE tb.id=? FOR UPDATE;", mapper, id)
            }

            override fun selectForUpdate(jdbcTemplate: JdbcTemplate, mapper: RowMapper<TilbakekrevingEntity>): List<TilbakekrevingEntity> {
                return jdbcTemplate.query("SELECT * FROM tilbakekreving JOIN tilbakekreving_behandling tb ON tilbakekreving.id=tb.tilbakekreving_id WHERE tb.id=? FOR UPDATE;", mapper, id)
            }
        }

        class EksternFagsakId(private val fagsakId: String, private val fagsystem: FagsystemDTO) : FindTilbakekrevingStrategy {
            private fun ytelse() = when (fagsystem) {
                FagsystemDTO.EF -> Ytelsestype.OVERGANGSSTØNAD
                FagsystemDTO.KONT -> Ytelsestype.KONTANTSTØTTE
                FagsystemDTO.IT01 -> Ytelsestype.INFOTRYGD
                FagsystemDTO.BA -> Ytelsestype.BARNETRYGD
                FagsystemDTO.TS -> Ytelsestype.TILLEGGSSTØNAD
                FagsystemDTO.AAP -> Ytelsestype.ARBEIDSAVKLARINGSPENGER
            }.name

            override fun select(jdbcTemplate: JdbcTemplate, mapper: RowMapper<TilbakekrevingEntity>): List<TilbakekrevingEntity> {
                return jdbcTemplate.query(
                    "SELECT * FROM tilbakekreving JOIN tilbakekreving_ekstern_fagsak ef ON tilbakekreving.id=ef.tilbakekreving_ref WHERE ef.ekstern_id=? AND ef.ytelse=?;",
                    mapper,
                    fagsakId,
                    ytelse(),
                )
            }

            override fun selectForUpdate(jdbcTemplate: JdbcTemplate, mapper: RowMapper<TilbakekrevingEntity>): List<TilbakekrevingEntity> {
                return jdbcTemplate.query(
                    "SELECT * FROM tilbakekreving JOIN tilbakekreving_ekstern_fagsak ef ON tilbakekreving.id=ef.tilbakekreving_ref WHERE ef.ekstern_id=? AND ef.ytelse=? FOR UPDATE;",
                    mapper,
                    fagsakId,
                    ytelse(),
                )
            }
        }

        class TilbakekrevingId(val id: String) : FindTilbakekrevingStrategy {
            override fun select(jdbcTemplate: JdbcTemplate, mapper: RowMapper<TilbakekrevingEntity>): List<TilbakekrevingEntity> {
                return jdbcTemplate.query(
                    "SELECT * FROM tilbakekreving WHERE id=?;",
                    mapper,
                    FieldConverter.NumericId.convert(id),
                )
            }

            override fun selectForUpdate(jdbcTemplate: JdbcTemplate, mapper: RowMapper<TilbakekrevingEntity>): List<TilbakekrevingEntity> {
                return jdbcTemplate.query(
                    "SELECT * FROM tilbakekreving WHERE id=? FOR UPDATE;",
                    mapper,
                    FieldConverter.NumericId.convert(id),
                )
            }
        }

        object TrengerPåminnelse : FindTilbakekrevingStrategy {
            override fun select(jdbcTemplate: JdbcTemplate, mapper: RowMapper<TilbakekrevingEntity>): List<TilbakekrevingEntity> {
                return jdbcTemplate.query(
                    "SELECT * FROM tilbakekreving WHERE neste_påminnelse IS NOT NULL AND neste_påminnelse < ?;",
                    mapper,
                    FieldConverter.LocalDateTimeConverter.convert(LocalDateTime.now()),
                )
            }

            override fun selectForUpdate(jdbcTemplate: JdbcTemplate, mapper: RowMapper<TilbakekrevingEntity>): List<TilbakekrevingEntity> {
                return jdbcTemplate.query(
                    "SELECT * FROM tilbakekreving WHERE neste_påminnelse IS NOT NULL AND neste_påminnelse < ? FOR UPDATE;",
                    mapper,
                    FieldConverter.LocalDateTimeConverter.convert(LocalDateTime.now()),
                )
            }
        }
    }
}
