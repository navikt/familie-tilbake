package no.nav.tilbakekreving.repository

import no.nav.tilbakekreving.entities.EksternFagsakBehandlingEntity
import no.nav.tilbakekreving.entities.EksternFagsakEntity
import no.nav.tilbakekreving.entities.UtvidetPeriodeEntity
import no.nav.tilbakekreving.entity.EksternFagsakBehandlingMapper
import no.nav.tilbakekreving.entity.EksternFagsakMapper
import no.nav.tilbakekreving.entity.Entity.Companion.get
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class NyEksternFagsakRepository(private val jdbcTemplate: JdbcTemplate) {
    fun hentEksternFagsak(tilbakekrevingId: String): EksternFagsakEntity {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_ekstern_fagsak WHERE tilbakekreving_ref = ?",
            tilbakekrevingId,
        ) { resultSet, _ ->
            val id = resultSet[EksternFagsakMapper.id]
            val behandlinger = hentBehandling(id)
            EksternFagsakMapper.map(resultSet, behandlinger)
        }.single()
    }

    fun lagre(eksternFagsak: EksternFagsakEntity) {
        EksternFagsakMapper.upsertQuery(jdbcTemplate, eksternFagsak)
        lagreBehandlinger(eksternFagsak.behandlinger)
    }

    private fun hentBehandling(eksternFagsakId: UUID): List<EksternFagsakBehandlingEntity> {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_ekstern_fagsak_behandling WHERE ekstern_fagsak_ref = ?",
            eksternFagsakId,
        ) { resultSet, _ ->
            val id = resultSet[EksternFagsakBehandlingMapper.id]
            val utvidetPerioder = hentUtvidetPerioder(id)
            EksternFagsakBehandlingMapper.map(resultSet, utvidetPerioder)
        }
    }

    private fun lagreBehandlinger(behandlinger: List<EksternFagsakBehandlingEntity>) {
        behandlinger.forEach { behandling ->
            EksternFagsakBehandlingMapper.upsertQuery(jdbcTemplate, behandling)
        }
    }

    private fun hentUtvidetPerioder(behandlingId: UUID): List<UtvidetPeriodeEntity> {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_ekstern_fagsak_behandling_utvidet_periode WHERE ekstern_fagsak_behandling_ref = ?",
            behandlingId,
        ) { resultSet, _ ->
            EksternFagsakBehandlingMapper.UtvidetPeriodeMapper.map(resultSet)
        }
    }
}
