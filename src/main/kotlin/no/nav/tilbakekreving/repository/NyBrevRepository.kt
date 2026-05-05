package no.nav.tilbakekreving.repository

import no.nav.tilbakekreving.entities.BrevEntity
import no.nav.tilbakekreving.entities.Brevtype
import no.nav.tilbakekreving.entities.VarselbrevEntity
import no.nav.tilbakekreving.entities.VedtaksbrevEntity
import no.nav.tilbakekreving.entity.BrevEntityMapper
import no.nav.tilbakekreving.entity.Entity.Companion.get
import no.nav.tilbakekreving.entity.FieldConverter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import java.util.UUID
import kotlin.collections.singleOrNull

@Repository
class NyBrevRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun hentBrev(tilbakekrevingId: String): List<BrevEntity> {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_brev WHERE tilbakekreving_ref = ?",
            FieldConverter.NumericId.convert(tilbakekrevingId),
        ) { resultSet, _ ->
            val id = resultSet[BrevEntityMapper.id]
            BrevEntityMapper.map(resultSet, hentVarselbrev(id), hentVedtaksbrev(id))
        }
    }

    fun hentVarselbrev(brevId: UUID): VarselbrevEntity? {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_varselbrev WHERE brev_ref = ?",
            brevId,
        ) { resultSet, _ ->
            BrevEntityMapper.VarselbrevEntityMapper.map(resultSet)
        }.singleOrNull()
    }

    fun hentVedtaksbrev(brevId: UUID): VedtaksbrevEntity? {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_vedtaksbrev WHERE brev_ref = ?",
            brevId,
        ) { resultSet, _ ->
            BrevEntityMapper.VedtaksbrevEntityMapper.map(resultSet)
        }.singleOrNull()
    }

    fun lagre(brevListe: List<BrevEntity>) {
        for (brev in brevListe) {
            jdbcTemplate.update("DELETE FROM tilbakekreving_brev WHERE tilbakekreving_ref=?;", brev.tilbakekrevingRef)
            BrevEntityMapper.upsertQuery(jdbcTemplate, brev)
            when (brev.brevtype) {
                Brevtype.VARSELBREV -> {
                    BrevEntityMapper.VarselbrevEntityMapper.upsertQuery(jdbcTemplate, brev.varselbrevEntity!!)
                }
                Brevtype.VEDTAKSBREV -> {
                    BrevEntityMapper.VedtaksbrevEntityMapper.upsertQuery(jdbcTemplate, brev.vedtaksbrevEntity!!)
                }
                Brevtype.VARSEL_BREV -> {
                    throw IllegalArgumentException("Deprecated enum")
                }
            }
        }
    }
}
