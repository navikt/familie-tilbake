package no.nav.tilbakekreving.vedtak

import com.fasterxml.jackson.core.type.TypeReference
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.tilbakekreving.entities.AktørEntity
import no.nav.tilbakekreving.fagsystem.Ytelsestype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsperiodeDto
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
            opprettet_tid,
            ytelsestype,
            kvittering,
            tilbakekrevingsperioder,
            behandlingstype
        ) VALUES (?, ?, ?, ?::jsonb, ?, ?, ?, ?::jsonb, ?)
            """.trimIndent(),
            iverksattVedtak.id,
            iverksattVedtak.behandlingId,
            iverksattVedtak.vedtakId,
            objectMapper.writeValueAsString(iverksattVedtak.aktør),
            iverksattVedtak.opprettetTid,
            iverksattVedtak.ytelsestype.name,
            iverksattVedtak.kvittering,
            objectMapper.writeValueAsString(iverksattVedtak.tilbakekrevingsperioder),
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
                opprettetTid = rs.getTimestamp("opprettet_tid").toLocalDateTime().toLocalDate(),
                ytelsestype = Ytelsestype.valueOf(rs.getString("ytelsestype")),
                kvittering = rs.getString("kvittering"),
                tilbakekrevingsperioder = objectMapper.readValue(
                    rs.getString("tilbakekrevingsperioder"),
                    object : TypeReference<List<TilbakekrevingsperiodeDto>>() {},
                ),
                behandlingstype = Behandlingstype.valueOf(rs.getString("behandlingstype")),
            )
        }
    }
}
