package no.nav.tilbakekreving.vedtak

import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.kravgrunnlag.KravgrunnlagRepository
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.entities.AktørEntity
import no.nav.tilbakekreving.entities.AktørType
import no.nav.tilbakekreving.fagsystem.Ytelsestype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.util.kroner
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.util.UUID

@Transactional
class VedtakDokumentRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var repository: VedtakDokumentRepository

    @Test
    fun `ny modell returnerer distinkte tilbakekrevinger fra kravgrunnlag med samme vedtak`() {
        val vedtakId = nyttDokumentVedtakId()
        val førsteTilbakekreving = opprettNyTilbakekreving()
        val andreTilbakekreving = opprettNyTilbakekreving()
        val annenVedtakTilbakekreving = opprettNyTilbakekreving()
        lagreNyttKravgrunnlag(førsteTilbakekreving, vedtakId)
        lagreNyttKravgrunnlag(førsteTilbakekreving, vedtakId)
        lagreNyttKravgrunnlag(andreTilbakekreving, vedtakId)
        lagreNyttKravgrunnlag(annenVedtakTilbakekreving, nyttDokumentVedtakId())

        val tilbakekrevingIder = repository.findTilbakekrevingIdsByVedtakId(vedtakId)

        tilbakekrevingIder.size shouldBe 2
        tilbakekrevingIder.toSet() shouldBe setOf(førsteTilbakekreving, andreTilbakekreving)
    }

    @Test
    fun `gammel modell returnerer distinkte behandlinger fra kravgrunnlag med samme vedtak`() {
        val vedtakId = nyttDokumentVedtakId()
        val førsteBehandling = opprettGammelBehandling(vedtakId)
        val andreBehandling = opprettGammelBehandling(vedtakId)
        opprettGammelBehandling(nyttDokumentVedtakId())
        kravgrunnlagRepository.insert(
            Testdata.lagKravgrunnlag(førsteBehandling).copy(vedtakId = vedtakId, aktiv = false),
        )

        val behandlingIder = repository.findBehandlingIdsByVedtakId(vedtakId)

        behandlingIder.size shouldBe 2
        behandlingIder.toSet() shouldBe setOf(førsteBehandling, andreBehandling)
    }

    @Test
    fun `vedtak uten kravgrunnlag gir ingen treff i noen modell`() {
        val vedtakId = nyttDokumentVedtakId()

        repository.findTilbakekrevingIdsByVedtakId(vedtakId) shouldBe emptyList()
        repository.findBehandlingIdsByVedtakId(vedtakId) shouldBe emptyList()
    }

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var kravgrunnlagRepository: KravgrunnlagRepository

    private fun opprettNyTilbakekreving(): String {
        val id = requireNotNull(
            jdbcTemplate.queryForObject("SELECT nextval('tilbakekreving_id')", Int::class.javaObjectType),
        )
        jdbcTemplate.update(
            "INSERT INTO tilbakekreving (id, nåværende_tilstand, opprettet, opprettelsesvalg) VALUES (?, ?, localtimestamp, ?)",
            id,
            "Testtilstand",
            "Testvalg",
        )
        return id.toString()
    }

    private fun lagreNyttKravgrunnlag(tilbakekrevingId: String, vedtakId: BigInteger) {
        jdbcTemplate.update(
            """
            INSERT INTO tilbakekreving_kravgrunnlag (
                id, tilbakekreving_id, vedtak_id, kravstatuskode, fagsystem_vedtaksdato, vedtak_gjelder_type,
                vedtak_gjelder_ident, utbetales_til_type, utbetales_til_ident, skal_beregne_renter,
                ansvarlig_enhet, kontrollfelt, kravgrunnlag_id, referanse, opprettet
            ) VALUES (?, ?, ?, 'NYTT', NULL, 'Person', ?, 'Person', ?, TRUE, '1234', 'kontroll', 'kravgrunnlag', 'referanse', localtimestamp)
            """.trimIndent(),
            UUID.randomUUID(),
            tilbakekrevingId.toInt(),
            vedtakId.toLong(),
            Testdata.STANDARD_BRUKERIDENT,
            Testdata.STANDARD_BRUKERIDENT,
        )
    }

    private fun opprettGammelBehandling(vedtakId: BigInteger): UUID {
        val fagsak = fagsakRepository.insert(Testdata.fagsak())
        val behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsak.id))
        kravgrunnlagRepository.insert(Testdata.lagKravgrunnlag(behandling.id).copy(vedtakId = vedtakId))
        return behandling.id
    }
}

internal fun nyttDokumentVedtakId(): BigInteger =
    BigInteger.valueOf(8_000_000_000_000_000_000L) + KravgrunnlagGenerator.nextId(18).toBigInteger()

internal fun iverksattVedtakForDokumentTest(
    behandlingId: UUID,
    vedtakId: BigInteger,
    nyModell: Boolean,
    ident: String,
): IverksattVedtak = IverksattVedtak(
    id = UUID.randomUUID(),
    behandlingId = behandlingId,
    nyModell = nyModell,
    vedtakId = vedtakId,
    aktør = AktørEntity(aktørType = AktørType.Person, ident = ident),
    ytelsestypeKode = Ytelsestype.TILLEGGSSTØNAD.kode,
    kvittering = "00",
    perioder = listOf(
        IverksattVedtak.IverksattPeriode(
            id = UUID.randomUUID(),
            fom = 1.januar(2025),
            tom = 31.januar(2025),
            beløpTilbakekreves = 2000.kroner,
            skattebeløp = 100.kroner,
            rentebeløp = 50.kroner,
        ),
    ),
    vedtaksdato = 10.januar(2025),
    behandlingstype = Behandlingstype.TILBAKEKREVING,
)
