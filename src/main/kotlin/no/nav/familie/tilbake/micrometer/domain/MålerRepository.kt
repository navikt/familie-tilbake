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
    @Query("""SELECT ytelsestype, 
                     extract(ISOYEAR from behandling.opprettet_dato) as år,  
                     extract(WEEK from behandling.opprettet_dato) as uke,
                     COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              WHERE status <> 'AVSLUTTET'
              GROUP BY ytelsestype, år, uke""")
    fun finnÅpneBehandlinger(): List<ForekomsterPerUke>

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
    @Query("""SELECT ytelsestype, 
                     b.brevtype, 
                     extract(ISOYEAR from b.opprettet_tid) as år,  
                     extract(WEEK from b.opprettet_tid) as uke,
                     COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              JOIN brevsporing b ON behandling.id = b.behandling_id
              GROUP BY ytelsestype, b.brevtype, år, uke""")
    fun finnSendteBrev(): List<BrevPerUke>

    // language=PostgreSQL
    @Query("""SELECT ytelsestype, 
                     behandlingsresultat.type as vedtakstype, 
                     extract(ISOYEAR from avsluttet_dato) as år,
                     extract(WEEK from avsluttet_dato) as uke,
                     COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              JOIN behandlingsresultat ON behandling.id = behandlingsresultat.behandling_id
              WHERE status = 'AVSLUTTET'
              GROUP BY ytelsestype, vedtakstype, år, uke""")
    fun finnVedtak(): List<VedtakPerUke>


}