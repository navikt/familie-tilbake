package no.nav.tilbakekreving.repository

import no.nav.tilbakekreving.entities.BeløpEntity
import no.nav.tilbakekreving.entities.KravgrunnlagHendelseEntity
import no.nav.tilbakekreving.entities.KravgrunnlagPeriodeEntity
import no.nav.tilbakekreving.entity.Entity.Companion.get
import no.nav.tilbakekreving.entity.KravgrunnlagBeløpMapper
import no.nav.tilbakekreving.entity.KravgrunnlagHendelseMapper
import no.nav.tilbakekreving.entity.KravgrunnlagPeriodeMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class NyKravgrunnlagRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun lagre(kravgrunnlagHendelser: List<KravgrunnlagHendelseEntity>) {
        for (kravgrunnlag in kravgrunnlagHendelser) {
            KravgrunnlagHendelseMapper.insertQuery(jdbcTemplate, kravgrunnlag)
            lagrePerioder(kravgrunnlag.perioder)
        }
    }

    fun hentKravgrunnlag(tilbakekrevingId: String): List<KravgrunnlagHendelseEntity> {
        return jdbcTemplate.query("SELECT * FROM tilbakekreving_kravgrunnlag WHERE tilbakekreving_id=?", tilbakekrevingId) { resultSet, _ ->
            KravgrunnlagHendelseMapper.map(resultSet, hentPerioder(resultSet[KravgrunnlagHendelseMapper.id]))
        }
    }

    private fun lagrePerioder(perioder: List<KravgrunnlagPeriodeEntity>) {
        for (periode in perioder) {
            KravgrunnlagPeriodeMapper.insertQuery(jdbcTemplate, periode)
            lagreBeløp(periode.beløp)
        }
    }

    private fun hentPerioder(kravgrunnlagId: UUID): List<KravgrunnlagPeriodeEntity> {
        return jdbcTemplate.query("SELECT * FROM tilbakekreving_kravgrunnlag_periode WHERE kravgrunnlag_id=?", kravgrunnlagId) { resultSet, _ ->
            KravgrunnlagPeriodeMapper.map(resultSet, hentBeløp(resultSet[KravgrunnlagPeriodeMapper.id]))
        }
    }

    private fun lagreBeløp(beløp: List<BeløpEntity>) {
        for (beløp in beløp) {
            KravgrunnlagBeløpMapper.insertQuery(jdbcTemplate, beløp)
        }
    }

    private fun hentBeløp(periodeId: UUID): List<BeløpEntity> {
        return jdbcTemplate.query("SELECT * FROM tilbakekreving_kravgrunnlag_beløp WHERE kravgrunnlag_periode_id=?", periodeId) { resultSet, _ ->
            KravgrunnlagBeløpMapper.map(resultSet)
        }
    }
}
