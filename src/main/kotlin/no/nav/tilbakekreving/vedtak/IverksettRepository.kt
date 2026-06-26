package no.nav.tilbakekreving.vedtak

import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.tilbakekreving.entities.AktørEntity
import no.nav.tilbakekreving.entity.FieldConverter
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.sql.PreparedStatement
import java.time.LocalDate
import java.util.UUID

@Repository
class IverksettRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    @Transactional
    fun lagreIverksattVedtak(iverksattVedtak: IverksattVedtak) {
        jdbcTemplate.update(
            """
        INSERT INTO iverksatt_vedtak (
            id,
            behandling_id,
            ny_modell,
            vedtak_id,
            aktør,
            ytelsestype,
            kvittering,
            vedtaksdato,
            behandlingstype
        ) VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?, ? )
            """.trimIndent(),
            iverksattVedtak.id,
            iverksattVedtak.behandlingId,
            iverksattVedtak.nyModell,
            iverksattVedtak.vedtakId,
            objectMapper.writeValueAsString(iverksattVedtak.aktør),
            iverksattVedtak.ytelsestypeKode,
            iverksattVedtak.kvittering,
            iverksattVedtak.vedtaksdato,
            iverksattVedtak.behandlingstype.name,
        )
        insertPeriode(iverksattVedtak.id, iverksattVedtak.perioder)
    }

    @Transactional
    fun insertPeriode(iverksattPeriodeId: UUID, perioder: List<IverksattVedtak.IverksattPeriode>) {
        jdbcTemplate.batchUpdate(
            "INSERT INTO iverksatt_vedtak_periode (id, iverksatt_vedtak_ref, fom, tom, beløp_tilbakekreves, skattebeløp, rentebeløp) VALUES (?, ? , ? , ? , ? , ? , ?)",
            object : BatchPreparedStatementSetter {
                override fun setValues(ps: PreparedStatement, i: Int) {
                    val periode = perioder[i]
                    ps.setObject(1, periode.id)
                    ps.setObject(2, iverksattPeriodeId)
                    ps.setObject(3, periode.fom)
                    ps.setObject(4, periode.tom)
                    ps.setString(5, FieldConverter.BigDecimalConverter.convert(periode.beløpTilbakekreves))
                    ps.setString(6, FieldConverter.BigDecimalConverter.convert(periode.skattebeløp))
                    ps.setString(7, FieldConverter.BigDecimalConverter.convert(periode.rentebeløp))
                }

                override fun getBatchSize() = perioder.size
            },
        )
    }

    fun hentIverksattVedtakMedVedtaksdato(vedtaksdato: LocalDate): List<IverksattVedtak> {
        return jdbcTemplate.query(
            "SELECT * FROM iverksatt_vedtak WHERE vedtaksdato = ?",
            vedtaksdato,
        ) { rs, _ ->
            val iverksattVedtakId = UUID.fromString(rs.getString("id"))
            IverksattVedtak(
                id = iverksattVedtakId,
                behandlingId = UUID.fromString(rs.getString("behandling_id")),
                nyModell = rs.getBoolean("ny_modell"),
                vedtakId = rs.getObject("vedtak_id", BigInteger::class.java),
                aktør = objectMapper.readValue(rs.getString("aktør"), AktørEntity::class.java),
                ytelsestypeKode = rs.getString("ytelsestype"),
                kvittering = rs.getString("kvittering"),
                perioder = hentPerioder(iverksattVedtakId),
                behandlingstype = Behandlingstype.valueOf(rs.getString("behandlingstype")),
                vedtaksdato = rs.getObject("vedtaksdato", LocalDate::class.java),
            )
        }
    }

    fun hentPerioder(iverksattVedtakId: UUID): List<IverksattVedtak.IverksattPeriode> {
        return jdbcTemplate.query("SELECT * FROM iverksatt_vedtak_periode WHERE iverksatt_vedtak_ref = ?", iverksattVedtakId) { rs, _ ->
            IverksattVedtak.IverksattPeriode(
                id = UUID.fromString(rs.getString("id")),
                fom = rs.getDate("fom").toLocalDate(),
                tom = rs.getDate("tom").toLocalDate(),
                beløpTilbakekreves = FieldConverter.BigDecimalConverter.required().convert(rs, "beløp_tilbakekreves"),
                skattebeløp = FieldConverter.BigDecimalConverter.required().convert(rs, "skattebeløp"),
                rentebeløp = FieldConverter.BigDecimalConverter.required().convert(rs, "rentebeløp"),
            )
        }
    }

    fun findByBehandlingId(behandlingId: UUID): IverksattVedtak? {
        val sql = "SELECT * FROM iverksatt_vedtak WHERE behandling_id = ?"

        return jdbcTemplate.query(sql, arrayOf(behandlingId)) { rs, _ ->
            val iverksattVedtakId = UUID.fromString(rs.getString("id"))
            IverksattVedtak(
                id = iverksattVedtakId,
                behandlingId = UUID.fromString(rs.getString("behandling_id")),
                nyModell = rs.getBoolean("ny_modell"),
                vedtakId = rs.getObject("vedtak_id", BigInteger::class.java),
                aktør = objectMapper.readValue(rs.getString("aktør"), AktørEntity::class.java),
                ytelsestypeKode = rs.getString("ytelsestype"),
                kvittering = rs.getString("kvittering"),
                perioder = hentPerioder(iverksattVedtakId),
                behandlingstype = Behandlingstype.valueOf(rs.getString("behandlingstype")),
                vedtaksdato = rs.getObject("vedtaksdato", LocalDate::class.java),
            )
        }.firstOrNull()
    }
}
