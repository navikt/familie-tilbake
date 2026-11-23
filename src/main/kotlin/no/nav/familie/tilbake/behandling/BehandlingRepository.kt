package no.nav.familie.tilbake.behandling

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.familie.tilbake.behandling.domain.Behandling
import no.nav.familie.tilbake.common.repository.InsertUpdateRepository
import no.nav.familie.tilbake.common.repository.RepositoryInterface
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Repository
@Transactional
interface BehandlingRepository :
    RepositoryInterface<Behandling, UUID>,
    InsertUpdateRepository<Behandling> {
    // language=PostgreSQL
    @Query(
        """
            SELECT beh.* FROM behandling beh JOIN fagsak f ON beh.fagsak_id = f.id 
             WHERE f.ytelsestype=:ytelsestype AND f.ekstern_fagsak_id=:eksternFagsakId
            AND beh.status <>'AVSLUTTET' AND beh.type='TILBAKEKREVING'
    """,
    )
    fun finnÅpenTilbakekrevingsbehandling(
        ytelsestype: Ytelsestype,
        eksternFagsakId: String,
    ): Behandling?

    @Query(
        """
            SELECT beh.* FROM behandling beh JOIN fagsak f ON beh.fagsak_id = f.id 
             WHERE f.ytelsestype=:ytelsestype AND f.ekstern_fagsak_id=:eksternFagsakId
             ORDER BY beh.opprettet_tid DESC LIMIT 1
        """,
    )
    fun finnNyesteTilbakekrevingsbehandlingForYtelsestypeAndEksternFagsakId(
        ytelsestype: Ytelsestype,
        eksternFagsakId: String,
    ): Behandling?

    // language=PostgreSQL
    @Query(
        """
            SELECT beh.* FROM behandling beh WHERE id=(SELECT arsak.behandling_id FROM behandlingsarsak arsak
            WHERE arsak.original_behandling_id=:behandlingId ORDER BY arsak.opprettet_tid DESC LIMIT 1)
            AND beh.status <>'AVSLUTTET' AND beh.type='REVURDERING_TILBAKEKREVING'
    """,
    )
    fun finnÅpenTilbakekrevingsrevurdering(behandlingId: UUID): Behandling?

    // language=PostgreSQL
    @Query(
        """
            SELECT beh.* FROM behandling beh JOIN fagsystemsbehandling fsb ON fsb.behandling_id= beh.id  JOIN fagsak fs ON fs.id= beh.fagsak_id
            WHERE fsb.ekstern_id=:eksternId AND fsb.aktiv=TRUE AND fs.fagsystem=:fagsystem
            AND beh.type='TILBAKEKREVING' AND beh.status='AVSLUTTET' ORDER BY beh.opprettet_tid DESC
    """,
    )
    fun finnAvsluttetTilbakekrevingsbehandlinger(
        eksternId: String,
        fagsystem: Fagsystem,
    ): List<Behandling>

    @Query(
        """
            SELECT b.id
            FROM behandling b JOIN fagsak f ON b.fagsak_id = f.id
            WHERE b.status != 'AVSLUTTET'
            AND b.opprettet_tid < :opprettetFørDato
            AND f.fagsystem = :fagsystem
        """,
    )
    fun finnÅpneBehandlingerOpprettetFør(
        fagsystem: Fagsystem,
        opprettetFørDato: LocalDateTime,
    ): List<UUID>?

    // language=PostgreSQL

    fun findByFagsakId(fagsakId: UUID): List<Behandling>

    // language=PostgreSQL
    @Query(
        """
            SELECT beh.* FROM behandling beh JOIN behandlingsstegstilstand tilstand ON tilstand.behandling_id = beh.id
            WHERE beh.type='TILBAKEKREVING' AND beh.status='UTREDES' AND
            tilstand.behandlingssteg='FAKTA' AND tilstand.behandlingsstegsstatus='KLAR'
    """,
    )
    fun finnAlleBehandlingerKlarForSaksbehandling(): List<Behandling>

    // language=PostgreSQL
    @Query(
        """
            SELECT beh.* FROM behandling beh 
            JOIN behandlingsstegstilstand tilstand ON tilstand.behandling_id = beh.id
            WHERE beh.type='TILBAKEKREVING' 
            AND beh.status='UTREDES' 
            AND tilstand.behandlingsstegsstatus='VENTER' 
            AND tilstand.behandlingssteg<>'GRUNNLAG' 
            AND tilstand.tidsfrist <= :dagensdato
    """,
    )
    fun finnAlleBehandlingerKlarForGjenoppta(dagensdato: LocalDate): List<Behandling>

    // language=PostgreSQL
    @Query(
        """
            SELECT b.* FROM behandlingsstegstilstand bst
            JOIN behandling b ON bst.behandling_id = b.id
            JOIN fagsak f ON b.fagsak_id = f.id
            WHERE f.fagsystem = :fagsystem AND bst.behandlingssteg = 'FATTE_VEDTAK' AND bst.behandlingsstegsstatus = 'TILBAKEFØRT' AND b.status != 'AVSLUTTET'
        """,
    )
    fun hentÅpneBehandlingerMedTilbakeførtFatteVedtakSteg(fagsystem: Fagsystem): List<Behandling>

    @Query(
        """
            SELECT beh.* FROM behandling beh
            LEFT JOIN Kravgrunnlag431 k431 ON beh.id = k431.behandling_id
            WHERE k431.id IS NULL
            AND beh.status != 'AVSLUTTET'
            AND beh.opprettet_dato < :utgaattDato
        """,
    )
    fun finnAlleGamleBehandlingerUtenKravgrunnlag(utgaattDato: LocalDate): List<Behandling>

    fun findByEksternBrukId(eksternBrukId: UUID): Behandling?

    @Query(
        """
            SELECT type,
                   COUNT(*) FILTER (WHERE opprettet_tid < TIMESTAMP '2026-01-01') AS antall2025,
                   COUNT(*) FILTER (WHERE opprettet_tid >= TIMESTAMP '2026-01-01') AS antall2026
            FROM behandlingsarsak
            WHERE opprettet_tid >= TIMESTAMP '2025-01-01'
              AND opprettet_tid < TIMESTAMP '2027-01-01'
            GROUP BY type
        """,
    )
    fun hentRevurderingsårsaksoversikt(): List<Revurderingsårsaksoversikt>
}

data class Revurderingsårsaksoversikt(
    val type: Behandlingsårsakstype,
    val antall2025: Long,
    val antall2026: Long,
) {
    @get:JsonProperty("totalt")
    val totalt: Long get() = antall2025 + antall2026

    companion object {
        fun medAlleÅrsakstyper(antall: List<Revurderingsårsaksoversikt>): List<Revurderingsårsaksoversikt> {
            val antallPerType = antall.associateBy { it.type }
            return Behandlingsårsakstype.entries.map { type ->
                antallPerType[type] ?: Revurderingsårsaksoversikt(type, 0, 0)
            }
        }
    }
}
