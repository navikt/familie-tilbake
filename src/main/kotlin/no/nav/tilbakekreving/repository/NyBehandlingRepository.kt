package no.nav.tilbakekreving.repository

import no.nav.tilbakekreving.entities.BehandlingEntity
import no.nav.tilbakekreving.entity.BehandlingEntityMapper
import no.nav.tilbakekreving.entity.Entity.Companion.get
import no.nav.tilbakekreving.entity.FieldConverter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.query
import org.springframework.stereotype.Repository

@Repository
class NyBehandlingRepository(
    private val jdbcTemplate: JdbcTemplate,
) {
    fun hentBehandlinger(
        tilbakekrevingId: String,
        jsonBehandlinger: List<BehandlingEntity>,
    ): List<BehandlingEntity> {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_behandling WHERE tilbakekreving_id = ?",
            FieldConverter.NumericId.convert(tilbakekrevingId),
        ) { resultSet, _ ->
            val jsonBehandling = jsonBehandlinger.single { it.id == resultSet[BehandlingEntityMapper.id] }
            BehandlingEntityMapper.map(
                resultSet = resultSet,
                enhet = jsonBehandling.enhet,
                ansvarligSaksbehandler = jsonBehandling.ansvarligSaksbehandler,
                foreldelsessteg = jsonBehandling.foreldelsestegEntity,
                faktasteg = jsonBehandling.faktastegEntity,
                vilkårsvurdering = jsonBehandling.vilkårsvurderingstegEntity,
                foreslåVedtak = jsonBehandling.foreslåVedtakStegEntity,
                fatteVedtak = jsonBehandling.fatteVedtakStegEntity,
                påVent = jsonBehandling.påVentEntity,
                brevmottakerSteg = jsonBehandling.brevmottakerStegEntity,
            )
        }
    }

    fun lagreBehandlinger(behandlinger: List<BehandlingEntity>) {
        for (behandling in behandlinger) {
            BehandlingEntityMapper.upsertQuery(jdbcTemplate, behandling)
        }
    }
}
