package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface BehandlingRepository : RepositoryInterface<Behandling, UUID>, InsertUpdateRepository<Behandling> {

    // language=PostgreSQL
    @Query("""
            SELECT beh.* FROM behandling beh JOIN fagsak f ON beh.fagsak_id = f.id 
             WHERE f.ytelsestype=:ytelsestype AND f.ekstern_fagsak_id=:eksternFagsakId
            AND beh.status <>'AVSLUTTET' AND beh.type='TILBAKEKREVING'
    """)
    fun finnÅpenTilbakekrevingsbehandling(ytelsestype: Ytelsestype,
                                          eksternFagsakId: String): Behandling?

    // language=PostgreSQL
    @Query("""
            SELECT beh.* FROM behandling beh JOIN fagsystemsbehandling fag ON fag.behandling_id= beh.id 
            WHERE fag.ekstern_id=:eksternId AND fag.aktiv=TRUE 
            AND beh.type='TILBAKEKREVING' AND beh.status='AVSLUTTET' ORDER BY beh.opprettet_tid DESC
    """)
    fun finnAvsluttetTilbakekrevingsbehandlinger(eksternId: String): List<Behandling>

    // language=PostgreSQL
    @Query("""
            SELECT beh.* FROM behandling beh JOIN fagsak f ON beh.fagsak_id = f.id 
             WHERE f.ytelsestype=:ytelsestype AND f.ekstern_fagsak_id=:eksternFagsakId
            AND beh.ekstern_bruk_id=:eksternBrukId
    """)
    fun findByYtelsestypeAndEksternFagsakIdAndEksternBrukId(ytelsestype: Ytelsestype,
                                                            eksternFagsakId: String,
                                                            eksternBrukId: UUID): Behandling?

    fun findByEksternBrukId(eksternBrukId: UUID): Behandling

    fun findByFagsakId(fagsakId: UUID): List<Behandling>
}
