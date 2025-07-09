package no.nav.tilbakekreving.vedtak

import com.fasterxml.jackson.core.type.TypeReference
import no.nav.familie.tilbake.common.repository.Endret
import no.nav.familie.tilbake.common.repository.Sporbar
import no.nav.familie.tilbake.common.repository.SporbarUtils
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.tilbakekreving.entities.AktørEntity
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsvedtakDto
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigInteger
import java.time.LocalDate
import java.util.UUID

@Repository
class IverksettRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun lagreIverksattVedtak(iverksattVedtak: IverksattVedtak) {
        jdbcTemplate.update(
            """
        INSERT INTO iverksatt_vedtak (
            id,
            behandling_id,
            vedtak_id,
            aktør,
            ytelsestype,
            kvittering,
            tilbakekrevingsvedtak,
            opprettet_av,
            opprettet_tid,
            endret_av,
            endret_tid,
            behandlingstype
        ) VALUES (?, ?, ?, ?::jsonb, ?, ?, ?::jsonb, ?, ?, ?, ?, ? )
            """.trimIndent(),
            iverksattVedtak.id,
            iverksattVedtak.behandlingId,
            iverksattVedtak.vedtakId,
            objectMapper.writeValueAsString(iverksattVedtak.aktør),
            iverksattVedtak.ytelsestypeKode,
            iverksattVedtak.kvittering,
            objectMapper.writeValueAsString(iverksattVedtak.tilbakekrevingsvedtak),
            iverksattVedtak.sporbar.opprettetAv,
            iverksattVedtak.sporbar.opprettetTid,
            iverksattVedtak.sporbar.endret.endretAv,
            iverksattVedtak.sporbar.endret.endretTid,
            iverksattVedtak.behandlingstype.name,
        )
    }

    fun hentIverksattVedtakMedOpprettetTid(opprettetTid: LocalDate): List<IverksattVedtak> {
        return jdbcTemplate.query(
            "SELECT * FROM iverksatt_vedtak WHERE DATE(opprettet_tid) = ?",
            arrayOf(opprettetTid),
        ) { rs, _ ->
            IverksattVedtak(
                id = UUID.fromString(rs.getString("id")),
                behandlingId = UUID.fromString(rs.getString("behandling_id")),
                vedtakId = rs.getObject("vedtak_id", BigInteger::class.java),
                aktør = objectMapper.readValue(rs.getString("aktør"), AktørEntity::class.java),
                ytelsestypeKode = rs.getString("ytelsestype"),
                kvittering = rs.getString("kvittering"),
                tilbakekrevingsvedtak = objectMapper.readValue(
                    rs.getString("tilbakekrevingsvedtak"),
                    object : TypeReference<TilbakekrevingsvedtakDto>() {},
                ),
                sporbar = Sporbar(
                    opprettetAv = rs.getString("opprettet_av"),
                    opprettetTid = rs.getTimestamp("opprettet_tid").toLocalDateTime(),
                    endret = Endret(
                        endretAv = rs.getString("endret_av") ?: "VL",
                        endretTid = rs.getTimestamp("endret_tid")?.toLocalDateTime() ?: SporbarUtils.now(),
                    ),
                ),
                behandlingstype = Behandlingstype.valueOf(rs.getString("behandlingstype")),
            )
        }
    }

    fun findByBehandlingId(behandlingId: UUID): IverksattVedtak? {
        val sql = "SELECT * FROM iverksatt_vedtak WHERE behandling_id = ?"

        return jdbcTemplate.query(sql, arrayOf(behandlingId)) { rs, _ ->
            IverksattVedtak(
                id = UUID.fromString(rs.getString("id")),
                behandlingId = UUID.fromString(rs.getString("behandling_id")),
                vedtakId = rs.getObject("vedtak_id", BigInteger::class.java),
                aktør = objectMapper.readValue(rs.getString("aktør"), AktørEntity::class.java),
                ytelsestypeKode = rs.getString("ytelsestype"),
                kvittering = rs.getString("kvittering"),
                tilbakekrevingsvedtak = objectMapper.readValue(
                    rs.getString("tilbakekrevingsvedtak"),
                    object : TypeReference<TilbakekrevingsvedtakDto>() {},
                ),
                sporbar = Sporbar(
                    opprettetAv = rs.getString("opprettet_av"),
                    opprettetTid = rs.getTimestamp("opprettet_tid").toLocalDateTime(),
                    endret = Endret(
                        endretAv = rs.getString("endret_av") ?: "VL",
                        endretTid = rs.getTimestamp("endret_tid")?.toLocalDateTime() ?: SporbarUtils.now(),
                    ),
                ),
                behandlingstype = Behandlingstype.valueOf(rs.getString("behandlingstype")),
            )
        }.firstOrNull()
    }
}
