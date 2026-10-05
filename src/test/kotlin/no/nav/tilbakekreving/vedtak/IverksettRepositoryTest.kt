package no.nav.tilbakekreving.vedtak

import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.OppslagSpringRunnerTest
import no.nav.familie.tilbake.data.Testdata
import no.nav.tilbakekreving.e2e.KravgrunnlagGenerator
import no.nav.tilbakekreving.entities.AktørEntity
import no.nav.tilbakekreving.entities.AktørType
import no.nav.tilbakekreving.fagsystem.Ytelsestype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.util.kroner
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import java.math.BigInteger
import java.util.UUID

@Transactional
class IverksettRepositoryTest : OppslagSpringRunnerTest() {
    @Autowired
    private lateinit var repository: IverksettRepository

    @Test
    fun `samme vedtakId i gammel og ny modell med et annet vedtak i databasen`() {
        val vedtakId = nyttDokumentVedtakId()
        val gammel = iverksattVedtakForDokumentTest(UUID.randomUUID(), vedtakId, false, Testdata.STANDARD_BRUKERIDENT)
        val ny = iverksattVedtakForDokumentTest(UUID.randomUUID(), vedtakId, true, Testdata.STANDARD_BRUKERIDENT).copy(
            kvittering = null,
            behandlingstype = Behandlingstype.REVURDERING_TILBAKEKREVING,
        )
        val annet = iverksattVedtakForDokumentTest(UUID.randomUUID(), nyttDokumentVedtakId(), true, Testdata.STANDARD_BRUKERIDENT)
        listOf(gammel, ny, annet).forEach(repository::lagreIverksattVedtak)

        val vedtak = repository.findByVedtakId(vedtakId)

        vedtak.size shouldBe 2
        vedtak.toSet() shouldBe setOf(gammel, ny)
    }

    @Test
    fun `vedtakId uten lagret vedtak`() {
        repository.findByVedtakId(nyttDokumentVedtakId()) shouldBe emptyList()
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
