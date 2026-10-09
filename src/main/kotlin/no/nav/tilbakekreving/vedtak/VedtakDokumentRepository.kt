package no.nav.tilbakekreving.vedtak

import org.springframework.dao.support.DataAccessUtils
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.math.BigInteger

@Repository
class VedtakDokumentRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun findTilbakekrevingIdByVedtakId(vedtakId: BigInteger): String? =
        DataAccessUtils.singleResult(
            jdbcTemplate.query(
                "SELECT DISTINCT tilbakekreving_id FROM tilbakekreving_kravgrunnlag WHERE vedtak_id = ?",
                arrayOf(vedtakId.longValueExact()),
            ) { rs, _ -> rs.getString("tilbakekreving_id") },
        )
}
