package no.nav.familie.tilbake.micrometer.domain

import no.nav.familie.tilbake.behandling.domain.Behandling
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
@Transactional
interface MålerRepository : CrudRepository<Behandling, UUID> {

    // language=PostgreSQL
    @Query("""SELECT ytelsestype, behandling.opprettet_dato as dato, COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              WHERE status <> 'AVSLUTTET'
              GROUP BY ytelsestype, behandling.opprettet_dato, opprettet_dato""")
    fun finnÅpneBehandlinger(): List<ForekomsterPerDato>

    // language=PostgreSQL
    @Query("""SELECT ytelsestype, behandlingssteg, COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              JOIN behandlingsstegstilstand b ON behandling.id = b.behandling_id
              WHERE status <> 'AVSLUTTET'
              AND behandlingsstegsstatus = 'KLAR'
              GROUP BY ytelsestype, behandlingssteg""")
    fun finnKlarTilBehandling(): List<BehandlingerPerSteg>

    // language=PostgreSQL
    @Query("""SELECT ytelsestype, behandlingssteg, COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              JOIN behandlingsstegstilstand b ON behandling.id = b.behandling_id
              WHERE status <> 'AVSLUTTET'
              AND behandlingsstegsstatus = 'VENTER'
              GROUP BY ytelsestype, behandlingssteg""")
    fun finnVentendeBehandlinger(): List<BehandlingerPerSteg>

    // language=PostgreSQL
    @Query("""SELECT ytelsestype, b.brevtype, b.opprettet_tid::DATE AS dato, COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              JOIN brevsporing b ON behandling.id = b.behandling_id
              GROUP BY ytelsestype, b.brevtype, dato""")
    fun finnSendteBrev(): List<BrevPerDato>

    // language=PostgreSQL
    @Query("""SELECT ytelsestype, behandlingsresultat.type as vedtakstype, avsluttet_dato as dato, COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              JOIN behandlingsresultat ON behandling.id = behandlingsresultat.behandling_id
              WHERE status = 'AVSLUTTET'
              GROUP BY ytelsestype, vedtakstype, avsluttet_dato""")
    fun finnVedtak(): List<VedtakPerDato>


}