package no.nav.familie.tilbake.micrometer.domain

import no.nav.familie.tilbake.behandling.domain.Behandling
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface StatistikkRepository : CrudRepository<Behandling, UUID> {

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
    @Query("""SELECT ytelsestype, vedtaksdato as dato, COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              JOIN behandlingsresultat ON behandling.id = behandlingsresultat.behandling_id
              AND behandlingsresultat.type = 'DELVIS_TILBAKEBETALING'
              JOIN behandlingsvedtak ON behandlingsresultat.id = behandlingsvedtak.behandlingsresultat_id
              AND behandlingsvedtak.iverksettingsstatus = 'IVERKSATT'
              WHERE status = 'AVSLUTTET'
              GROUP BY ytelsestype, vedtaksdato""")
    fun finnVedtakDelvisTilbakebetaling(): List<ForekomsterPerDato>

    // language=PostgreSQL
    @Query("""SELECT ytelsestype, vedtaksdato as dato, COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              JOIN behandlingsresultat ON behandling.id = behandlingsresultat.behandling_id
              AND behandlingsresultat.type = 'FULL_TILBAKEBETALING'
              JOIN behandlingsvedtak ON behandlingsresultat.id = behandlingsvedtak.behandlingsresultat_id
              AND behandlingsvedtak.iverksettingsstatus = 'IVERKSATT'
              WHERE status = 'AVSLUTTET'
              GROUP BY ytelsestype, vedtaksdato""")
    fun finnVedtakFullTilbakebetaling(): List<ForekomsterPerDato>

    // language=PostgreSQL
    @Query("""SELECT ytelsestype, vedtaksdato as dato, COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              JOIN behandlingsresultat ON behandling.id = behandlingsresultat.behandling_id
              AND behandlingsresultat.type = 'INGEN_TILBAKEBETALING'
              JOIN behandlingsvedtak ON behandlingsresultat.id = behandlingsvedtak.behandlingsresultat_id
              AND behandlingsvedtak.iverksettingsstatus = 'IVERKSATT'
              WHERE status = 'AVSLUTTET'
              GROUP BY ytelsestype, vedtaksdato""")
    fun finnVedtakIngenTilbakebetaling(): List<ForekomsterPerDato>

    // language=PostgreSQL
    @Query("""SELECT ytelsestype, vedtaksdato as dato, COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              JOIN behandlingsresultat ON behandling.id = behandlingsresultat.behandling_id
              AND behandlingsresultat.type LIKE 'HENLAGT%'
              JOIN behandlingsvedtak ON behandlingsresultat.id = behandlingsvedtak.behandlingsresultat_id
              AND behandlingsvedtak.iverksettingsstatus = 'IVERKSATT'
              WHERE status = 'AVSLUTTET'
              GROUP BY ytelsestype, vedtaksdato""")
    fun finnVedtakHenlagte(): List<ForekomsterPerDato>

    // language=PostgreSQL
    @Query("""SELECT ytelsestype, opprettet_tid::DATE AS dato, COUNT(*) AS antall
              FROM okonomi_xml_mottatt_arkiv
              GROUP BY ytelsestype, dato""")
    fun finnKobledeKravgrunnlag(): List<ForekomsterPerDato>

    // language=PostgreSQL
    @Query("""SELECT ytelsestype, opprettet_tid::DATE AS dato, COUNT(*) AS antall
              FROM okonomi_xml_mottatt
              GROUP BY ytelsestype, dato""")
    fun finnUkobledeKravgrunnlag(): List<ForekomsterPerDato>

    // language=PostgreSQL
    @Query("""SELECT ytelsestype, b.opprettet_tid::DATE AS dato, COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              JOIN brevsporing b ON behandling.id = b.behandling_id
              AND b.brevtype = 'VARSEL'
              GROUP BY ytelsestype, dato""")
    fun finnSendteVarselbrev(): List<ForekomsterPerDato>

    // language=PostgreSQL
    @Query("""SELECT ytelsestype, b.opprettet_tid::DATE AS dato, COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              JOIN brevsporing b ON behandling.id = b.behandling_id
              AND b.brevtype = 'KORRIGERT_VARSEL'
              GROUP BY ytelsestype, dato""")
    fun finnSendteKorrigerteVarselbrev(): List<ForekomsterPerDato>

    // language=PostgreSQL
    @Query("""SELECT ytelsestype, b.opprettet_tid::DATE AS dato, COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              JOIN brevsporing b ON behandling.id = b.behandling_id
              AND b.brevtype = 'VEDTAK'
              GROUP BY ytelsestype, dato""")
    fun finnSendteVedtaksbrev(): List<ForekomsterPerDato>

    // language=PostgreSQL
    @Query("""SELECT ytelsestype, b.opprettet_tid::DATE AS dato, COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              JOIN brevsporing b ON behandling.id = b.behandling_id
              AND b.brevtype = 'HENLEGGELSE'
              GROUP BY ytelsestype, dato""")
    fun finnSendteHenleggelsesbrev(): List<ForekomsterPerDato>

    // language=PostgreSQL
    @Query("""SELECT ytelsestype, b.opprettet_tid::DATE AS dato, COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              JOIN brevsporing b ON behandling.id = b.behandling_id
              AND b.brevtype = 'INNHENT_DOKUMENTASJON'
              GROUP BY ytelsestype, dato""")
    fun finnSendteInnhentDokumentasjonsbrev(): List<ForekomsterPerDato>

    // language=PostgreSQL
    @Query("""SELECT ytelsestype, b.brevtype, b.opprettet_tid::DATE AS dato, COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              JOIN brevsporing b ON behandling.id = b.behandling_id
              GROUP BY ytelsestype, b.brevtype, dato""")
    fun finnSendteBrev(): List<BrevPerDato>

    // language=PostgreSQL
    @Query("""SELECT ytelsestype, behandlingsresultat.type as vedtakstype, vedtaksdato as dato, COUNT(*) AS antall
              FROM fagsak
              JOIN behandling ON fagsak.id = behandling.fagsak_id
              JOIN behandlingsresultat ON behandling.id = behandlingsresultat.behandling_id
              JOIN behandlingsvedtak ON behandlingsresultat.id = behandlingsvedtak.behandlingsresultat_id
              AND behandlingsvedtak.iverksettingsstatus = 'IVERKSATT'
              WHERE status = 'AVSLUTTET'
              GROUP BY ytelsestype, vedtakstype, vedtaksdato""")
    fun finnVedtak(): List<VedtakPerDato>


}