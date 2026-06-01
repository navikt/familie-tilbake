package no.nav.tilbakekreving.repository

import io.micrometer.core.instrument.Metrics
import io.micrometer.core.instrument.Timer
import no.nav.tilbakekreving.behandlingslogg.Behandlingslogg
import no.nav.tilbakekreving.entities.LoggInnlagEntity
import no.nav.tilbakekreving.entities.TilbakekrevingEntity
import no.nav.tilbakekreving.entity.Entity.Companion.get
import no.nav.tilbakekreving.entity.FieldConverter
import no.nav.tilbakekreving.entity.TilbakekrevingEntityMapper
import no.nav.tilbakekreving.kontrakter.tilstand.TilbakekrevingTilstand
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.time.LocalDateTime

@Repository
class TilbakekrevingRepository(
    private val jdbcTemplate: JdbcTemplate,
    private val behandlingRepository: NyBehandlingRepository,
    private val kravgrunnlagRepository: NyKravgrunnlagRepository,
    private val eksternFagsakRepository: NyEksternFagsakRepository,
    private val brevRepository: NyBrevRepository,
    private val brukerRepository: NyBrukerRepository,
    private val behandlingsloggRepository: NyBehandlingsloggRepository,
) {
    private val modelReadTimer = Timer.builder("tilbakekreving_load_time")
        .publishPercentiles(0.5, 0.9, 0.95, 0.99)
        .tag("readonly", "true")
        .register(Metrics.globalRegistry)
    private val modelWriteTimer = Timer.builder("tilbakekreving_load_time")
        .publishPercentiles(0.5, 0.9, 0.95, 0.99)
        .tag("readonly", "false")
        .register(Metrics.globalRegistry)

    fun nesteId(): String {
        return jdbcTemplate.query("SELECT nextval('tilbakekreving_id')") { rs, _ ->
            FieldConverter.NumericId.convert(rs, "nextval")
        }.single()
    }

    private fun hentTilbakekrevingEntity(
        resultSet: ResultSet,
    ): TilbakekrevingEntity {
        val id = resultSet[TilbakekrevingEntityMapper.id]
        return TilbakekrevingEntityMapper.map(
            resultSet = resultSet,
            eksternFagsak = eksternFagsakRepository.hentEksternFagsak(id),
            behandlingHistorikk = behandlingRepository.hentBehandlinger(id),
            kravgrunnlagHistorikk = kravgrunnlagRepository.hentKravgrunnlag(id),
            brevHistorikk = brevRepository.hentBrev(id),
            bruker = brukerRepository.hentBruker(id),
        )
    }

    fun hentTilbakekreving(strategy: TilbakekrevingFilter): TilbakekrevingEntity? {
        return modelReadTimer.record<TilbakekrevingEntity?> {
            hentTilbakekrevinger(strategy).firstOrNull()
        }
    }

    fun hentTilbakekrevinger(strategy: TilbakekrevingFilter): List<TilbakekrevingEntity> {
        return strategy.select(jdbcTemplate) { resultSet, index ->
            hentTilbakekrevingEntity(resultSet)
        }
    }

    fun hentAlleTilbakekrevinger(): List<TilbakekrevingEntity>? {
        return jdbcTemplate.query("SELECT * FROM tilbakekreving") { rs, _ ->
            hentTilbakekrevingEntity(rs)
        }
    }

    fun hentBehandlingslogg(tilbakekrevingId: String): Behandlingslogg {
        val entities = behandlingsloggRepository.hentBehandlingslogg(tilbakekrevingId)
        return Behandlingslogg(entities.map(LoggInnlagEntity::fraEntity).toMutableList())
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun opprett(
        tilbakekrevingEntity: TilbakekrevingEntity,
        behandlingslogg: Behandlingslogg,
    ): String {
        TilbakekrevingEntityMapper.insertQuery(
            jdbcTemplate,
            tilbakekrevingEntity,
        )
        lagreUnderobjekter(tilbakekrevingEntity, behandlingslogg)

        return tilbakekrevingEntity.id
    }

    // Denne er inline så den blir inlinet i transactions
    private inline fun hentForOppdatering(strategy: TilbakekrevingFilter): TilbakekrevingEntity? {
        return strategy.selectForUpdate(jdbcTemplate) { rs, _ ->
            hentTilbakekrevingEntity(rs)
        }.toList().firstOrNull()
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun hentOgLagreResultat(
        strategy: TilbakekrevingFilter,
        callback: (TilbakekrevingEntity, Behandlingslogg) -> TilbakekrevingEntity,
    ) {
        modelWriteTimer.record<Unit> {
            val entity = hentForOppdatering(strategy) ?: return@record
            val behandlingslogg = hentBehandlingslogg(entity.id)
            val oppdatertEntity = callback(entity, behandlingslogg)
            TilbakekrevingEntityMapper.updateQuery(
                jdbcTemplate,
                oppdatertEntity,
            )
            lagreUnderobjekter(oppdatertEntity, behandlingslogg)
        }
    }

    private fun lagreUnderobjekter(entity: TilbakekrevingEntity, behandlingslogg: Behandlingslogg) {
        eksternFagsakRepository.lagre(entity.eksternFagsak)
        kravgrunnlagRepository.lagre(entity.kravgrunnlagHistorikkEntities)
        behandlingRepository.lagreBehandlinger(entity.behandlingHistorikkEntities)
        brevRepository.lagre(entity.brevHistorikkEntities, entity.id)
        entity.bruker?.let { brukerRepository.lagre(it) }
        behandlingsloggRepository.lagre(behandlingslogg.tilEntity(entity.id), entity.id)
    }

    fun antallSakerPerTilstand(): List<ForenkletTilstandStatistikk> {
        return jdbcTemplate.query("SELECT t.nåværende_tilstand, ef.ytelse, COUNT(*) AS antall_saker FROM tilbakekreving t JOIN tilbakekreving_ekstern_fagsak ef ON t.id = ef.tilbakekreving_ref GROUP BY nåværende_tilstand, ef.ytelse;") { resultSet, _ ->
            ForenkletTilstandStatistikk(
                tilstand = resultSet.getString("nåværende_tilstand"),
                ytelse = resultSet.getString("ytelse"),
                antallSaker = resultSet.getInt("antall_saker"),
            )
        }
    }

    fun oppdaterNestePåminnelse(tilstand: TilbakekrevingTilstand) {
        jdbcTemplate.update(
            "UPDATE tilbakekreving SET neste_påminnelse=? WHERE nåværende_tilstand=?;",
            FieldConverter.LocalDateTimeConverter.convert(LocalDateTime.now()),
            FieldConverter.EnumConverter.of<TilbakekrevingTilstand>().convert(tilstand),
        )
    }

    data class ForenkletTilstandStatistikk(
        val tilstand: String,
        val ytelse: String,
        val antallSaker: Int,
    )
}
