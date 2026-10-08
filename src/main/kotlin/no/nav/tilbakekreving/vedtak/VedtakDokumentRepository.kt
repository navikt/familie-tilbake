package no.nav.tilbakekreving.vedtak

import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigInteger
import java.util.UUID

@Repository
class VedtakDokumentRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun findTilbakekrevingIdByVedtakId(vedtakId: BigInteger): String? =
        DataAccessUtils.singleResult(
            jdbcTemplate.query(
                "SELECT DISTINCT tilbakekreving_id FROM tilbakekreving_kravgrunnlag WHERE vedtak_id = ?",
                arrayOf(vedtakId.toLong()),
            ) { rs, _ -> rs.getString("tilbakekreving_id") },
        )

    fun findBehandlingIdsByVedtakId(vedtakId: BigInteger): List<UUID> =
        jdbcTemplate.query(
            "SELECT DISTINCT behandling_id FROM kravgrunnlag431 WHERE vedtak_id = ? AND behandling_id IS NOT NULL",
            arrayOf(vedtakId.toString()),
        ) { rs, _ -> UUID.fromString(rs.getString("behandling_id")) }
}
