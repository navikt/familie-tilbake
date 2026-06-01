package no.nav.tilbakekreving.repository

import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.entities.TilbakekrevingEntity
import no.nav.tilbakekreving.entity.FieldConverter
import no.nav.tilbakekreving.fagsystem.Ytelsestype
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import java.time.LocalDateTime
import java.util.UUID

sealed interface TilbakekrevingFilter {
    fun select(jdbcTemplate: JdbcTemplate, mapper: RowMapper<TilbakekrevingEntity>): List<TilbakekrevingEntity>

    fun selectForUpdate(jdbcTemplate: JdbcTemplate, mapper: RowMapper<TilbakekrevingEntity>): List<TilbakekrevingEntity>

    fun logContext(): SecureLog.Context

    private class BehandlingId(val id: UUID) : TilbakekrevingFilter {
        override fun select(jdbcTemplate: JdbcTemplate, mapper: RowMapper<TilbakekrevingEntity>): List<TilbakekrevingEntity> {
            return jdbcTemplate.query("SELECT * FROM tilbakekreving JOIN tilbakekreving_behandling tb ON tilbakekreving.id=tb.tilbakekreving_id WHERE tb.id=? FOR UPDATE;", mapper, id)
        }

        override fun selectForUpdate(jdbcTemplate: JdbcTemplate, mapper: RowMapper<TilbakekrevingEntity>): List<TilbakekrevingEntity> {
            return jdbcTemplate.query("SELECT * FROM tilbakekreving JOIN tilbakekreving_behandling tb ON tilbakekreving.id=tb.tilbakekreving_id WHERE tb.id=? FOR UPDATE;", mapper, id)
        }

        override fun logContext(): SecureLog.Context = SecureLog.Context.medBehandling(null, id.toString())
    }

    private class EksternFagsakId(private val fagsakId: String, private val fagsystem: FagsystemDTO) : TilbakekrevingFilter {
        private fun ytelse() = when (fagsystem) {
            FagsystemDTO.EF -> Ytelsestype.OVERGANGSSTØNAD
            FagsystemDTO.KONT -> Ytelsestype.KONTANTSTØTTE
            FagsystemDTO.IT01 -> Ytelsestype.INFOTRYGD
            FagsystemDTO.BA -> Ytelsestype.BARNETRYGD
            FagsystemDTO.TS -> Ytelsestype.TILLEGGSSTØNAD
            FagsystemDTO.AAP -> Ytelsestype.ARBEIDSAVKLARINGSPENGER
            FagsystemDTO.TP -> Ytelsestype.TILTAKSPENGER
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

        override fun logContext(): SecureLog.Context = SecureLog.Context.utenBehandling(fagsakId)
    }

    private class TilbakekrevingId(val id: String) : TilbakekrevingFilter {
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

        override fun logContext(): SecureLog.Context = SecureLog.Context.tom()
    }

    private object TrengerPåminnelse : TilbakekrevingFilter {
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

        override fun logContext(): SecureLog.Context = SecureLog.Context.tom()
    }

    companion object {
        fun behandling(id: UUID): TilbakekrevingFilter = BehandlingId(id)

        fun fagsak(fagsakId: String, fagsystem: FagsystemDTO): TilbakekrevingFilter = EksternFagsakId(fagsakId, fagsystem)

        fun tilbakekreving(id: String): TilbakekrevingFilter = TilbakekrevingId(id)

        fun trengerPåminnelse(): TilbakekrevingFilter = TrengerPåminnelse
    }
}
