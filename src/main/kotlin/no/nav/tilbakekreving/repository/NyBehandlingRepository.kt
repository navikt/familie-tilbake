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
    private val faktavurderingRepository: NyFaktavurderingRepository,
    private val foreldelseRepository: NyForeldelseRepository,
    private val fatteVedtakRepository: NyFatteVedtakRepository,
    private val foreslåVedtakRepository: NyForeslåVedtakRepository,
    private val vilkårsvurderingRepository: NyVilkårsvurderingRepository,
    private val brevmottakerRepository: NyBrevmottakerRepository,
    private val påventRepository: NyPåventRepository,
    private val uttalelseRepository: NyUttalelseRepository,
    private val forhåndsvarselUnntakRepository: NyForhåndsvarselUnntakRepository,
    private val utsettUttalelseRepository: NyUtsettUttalelseRepository,
) {
    fun hentBehandlinger(
        tilbakekrevingId: String,
        jsonBehandlinger: List<BehandlingEntity>,
    ): List<BehandlingEntity> {
        return jdbcTemplate.query(
            "SELECT * FROM tilbakekreving_behandling WHERE tilbakekreving_id = ?",
            FieldConverter.NumericId.convert(tilbakekrevingId),
        ) { resultSet, _ ->
            val behandlingId = resultSet[BehandlingEntityMapper.id]
            val jsonBehandling = jsonBehandlinger.single { it.id == behandlingId }
            BehandlingEntityMapper.map(
                resultSet = resultSet,
                foreldelsessteg = foreldelseRepository.hentForeldelsesvurdering(behandlingId),
                faktasteg = faktavurderingRepository.hentFaktavurdering(behandlingId),
                vilkårsvurdering = vilkårsvurderingRepository.hentVilkårsvurdering(behandlingId) ?: jsonBehandling.vilkårsvurderingstegEntity,
                foreslåVedtak = foreslåVedtakRepository.hentForeslåttVedtak(behandlingId) ?: jsonBehandling.foreslåVedtakStegEntity,
                fatteVedtak = fatteVedtakRepository.hentVedtaksvurdering(behandlingId) ?: jsonBehandling.fatteVedtakStegEntity,
                påVent = påventRepository.hentPåventetBehandling(behandlingId) ?: jsonBehandling.påVentEntity,
                brevmottakerSteg = brevmottakerRepository.hentBrevmottaker(behandlingId) ?: jsonBehandling.brevmottakerStegEntity,
                brukeruttalelseEntity = uttalelseRepository.hentBrukerUttalelsen(behandlingId),
                forhåndsvarselUnntak = forhåndsvarselUnntakRepository.hentForhåndsvarselUnntak(behandlingId),
                fristUtsettelse = utsettUttalelseRepository.hentUtsettUttalelseFrist(behandlingId),
            )
        }
    }

    fun lagreBehandlinger(behandlinger: List<BehandlingEntity>) {
        for (behandling in behandlinger) {
            BehandlingEntityMapper.upsertQuery(jdbcTemplate, behandling)
            foreldelseRepository.lagre(behandling.foreldelsestegEntity)
            faktavurderingRepository.lagre(behandling.faktastegEntity)
            fatteVedtakRepository.lagre(behandling.fatteVedtakStegEntity)
            vilkårsvurderingRepository.lagre(behandling.vilkårsvurderingstegEntity)
            foreslåVedtakRepository.lagre(behandling.foreslåVedtakStegEntity)
            påventRepository.lagre(behandling.påVentEntity, behandling.id)
            behandling.brevmottakerStegEntity?.let { brevmottakerRepository.lagre(it) }
            behandling.forhåndsvarselEntity?.let {
                it.brukeruttalelseEntity?.let { uttalelseRepository.lagre(it) }
                it.forhåndsvarselUnntakEntity?.let { forhåndsvarselUnntakRepository.lagre(it) }
                it.fristUtsettelseEntity.let { utsettUttalelseRepository.lagre(it) }
            }
        }
    }
}
