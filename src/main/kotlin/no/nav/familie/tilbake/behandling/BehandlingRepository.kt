package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.behandling.domain.Ytelsestype
import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BehandlingRepository : RepositoryInterface<Behandling, UUID>, InsertUpdateRepository<Behandling> {

    // language=PostgreSQL
    @Query("""
            select beh.* from behandling beh join fagsak f on beh.fagsak_id = f.id 
             where f.ytelsestype=:ytelsestype and f.ekstern_fagsak_id=:eksternFagsakId
            and beh.status <>'AVSLUTTET' and beh.type='TILBAKEKREVING'
    """)
    fun finnÅpenTilbakekrevingsbehandling(
            ytelsestype: Ytelsestype,
            eksternFagsakId: String
    ): Behandling?

    // language=PostgreSQL
    @Query("""
            select beh.* from behandling beh join ekstern_behandling eks on eks.behandling_id= beh.id 
            where eks.ekstern_id=:eksternId and eks.aktiv=true 
            and beh.type='TILBAKEKREVING' and beh.status='AVSLUTTET' ORDER BY beh.opprettet_tid DESC
    """)
    fun finnAvsluttetTilbakekrevingsbehandlinger(eksternId: String): List<Behandling>
}
