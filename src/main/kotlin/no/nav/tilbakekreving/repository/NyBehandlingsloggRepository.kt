package no.nav.tilbakekreving.repository

import no.nav.tilbakekreving.entities.LoggInnlagEntity
import no.nav.tilbakekreving.entity.BehandlingsloggMapper
import no.nav.tilbakekreving.entity.FieldConverter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository

@Repository
class NyBehandlingsloggRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun hentBehandlingslogg(tilbakekrevingId: String): List<LoggInnlagEntity> {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_behandlingslogg WHERE tilbakekreving_ref=?",
            FieldConverter.NumericId.convert(tilbakekrevingId),
        ) { resultSet, _ ->
            BehandlingsloggMapper.map(resultSet)
        }
    }

    fun lagre(behandlingslogg: List<LoggInnlagEntity>) {
        for (behandlingslogg in behandlingslogg) {
            BehandlingsloggMapper.insertQuery(jdbcTemplate, behandlingslogg)
        }
    }
}
