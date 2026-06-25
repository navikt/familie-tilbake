package no.nav.tilbakekreving.avstemmingMediator

import io.kotest.matchers.shouldBe
import no.nav.familie.tilbake.behandling.BehandlingRepository
import no.nav.familie.tilbake.behandling.FagsakRepository
import no.nav.familie.tilbake.common.repository.Sporbar
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.data.Testdata
import no.nav.familie.tilbake.iverksettvedtak.domain.ØkonomiXmlSendt
import no.nav.familie.tilbake.iverksettvedtak.ØkonomiXmlSendtRepository
import no.nav.familie.tilbake.kontrakter.objectMapper
import no.nav.tilbakekreving.e2e.E2EBase
import no.nav.tilbakekreving.entities.AktørEntity
import no.nav.tilbakekreving.entities.AktørType
import no.nav.tilbakekreving.fagsystem.Ytelsestype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.test.januar
import no.nav.tilbakekreving.test.mai
import no.nav.tilbakekreving.typer.v1.MmelDto
import no.nav.tilbakekreving.util.kroner
import no.nav.tilbakekreving.vedtak.IverksattVedtak
import no.nav.tilbakekreving.vedtak.IverksettRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.util.UUID

class AvstemmingMediatorTest : E2EBase() {
    @Autowired
    private lateinit var behandlingRepository: BehandlingRepository

    @Autowired
    private lateinit var fagsakRepository: FagsakRepository

    @Autowired
    lateinit var avstemmingMediator: AvstemmingMediator

    @Autowired
    lateinit var iverksettRepository: IverksettRepository

    @Autowired
    lateinit var økonomiXmlSendtRepository: ØkonomiXmlSendtRepository

    @Test
    fun `test avstemming`() {
        val opprettetTid = LocalDate.of(2025, 1, 10)

        val fagsak = fagsakRepository.insert(Testdata.fagsak())
        val behandling = behandlingRepository.insert(Testdata.lagBehandling(fagsakId = fagsak.id))
        val xml = readXml("/tilbakekrevingsvedtak/tilbakekrevingsvedtak.xml")
        val økonomiXmlSendt = ØkonomiXmlSendt(
            behandlingId = behandling.id,
            melding = xml,
            kvittering = objectMapper.writeValueAsString(lagMmmelDto("00", "OK")),
            sporbar =
                Sporbar(
                    opprettetAv = Constants.BRUKER_ID_VEDTAKSLØSNINGEN,
                    opprettetTid = opprettetTid.atStartOfDay(),
                ),
        )

        val iverksattVedtak1 = IverksattVedtak(
            id = UUID.fromString("11111111-2222-3333-4444-55555555555a"),
            behandlingId = UUID.fromString("11111111-2222-3333-4444-55555555555b"),
            nyModell = true,
            vedtakId = BigInteger("1234"),
            aktør = AktørEntity(aktørType = AktørType.Person, "11223344556"),
            ytelsestypeKode = Ytelsestype.TILLEGGSSTØNAD.kode,
            kvittering = "04",
            perioder = lagTilbakekrevingsvedtakDto(3000.00.kroner, 3.kroner, 100.kroner),
            vedtaksdato = opprettetTid,
            behandlingstype = Behandlingstype.TILBAKEKREVING,
        )

        val iverksattVedtak2 = IverksattVedtak(
            id = UUID.fromString("11111111-2222-3333-4444-5555555555aa"),
            behandlingId = UUID.fromString("11111111-2222-3333-4444-5555555555bb"),
            nyModell = true,
            vedtakId = BigInteger("1235"),
            aktør = AktørEntity(aktørType = AktørType.Person, "11223344557"),
            ytelsestypeKode = Ytelsestype.TILLEGGSSTØNAD.kode,
            kvittering = "00",
            perioder = lagTilbakekrevingsvedtakDto(0.kroner, 0.kroner, 0.kroner),
            vedtaksdato = opprettetTid,
            behandlingstype = Behandlingstype.REVURDERING_TILBAKEKREVING,
        )

        økonomiXmlSendtRepository.insert(økonomiXmlSendt)
        iverksettRepository.lagreIverksattVedtak(iverksattVedtak = iverksattVedtak1)
        iverksettRepository.lagreIverksattVedtak(iverksattVedtak = iverksattVedtak2)

        val result = avstemmingMediator.avstem(dato = opprettetTid)

        val expected = """
            avsender;vedtakId;fnr;vedtaksdato;fagsakYtelseType;tilbakekrevesBruttoUtenRenter;skatt;tilbakekrevesNettoUtenRenter;renter;erOmgjøringTilIngenTilbakekreving
            familie-tilbake;1234;11223344556;20250110;TSO;6000;6;5994;200;
            familie-tilbake;1235;11223344557;20250110;TSO;0;0;0;0;Omgjoring0
            familie-tilbake;0;32132132111;20250110;BA;2108;0;2108;0;
        """.trimIndent().trimEnd()

        result?.toString(Charsets.UTF_8) shouldBe expected
    }

    companion object {
        fun lagTilbakekrevingsvedtakDto(
            tilbakekrevingsBeløp: BigDecimal,
            skatt: BigDecimal,
            renter: BigDecimal,
        ): List<IverksattVedtak.IverksattPeriode> {
            return listOf(
                IverksattVedtak.IverksattPeriode(
                    id = UUID.randomUUID(),
                    fom = 1.januar(2021),
                    tom = 31.januar(2021),
                    beløpTilbakekreves = tilbakekrevingsBeløp,
                    skattebeløp = skatt,
                    rentebeløp = renter,
                ),
                IverksattVedtak.IverksattPeriode(
                    id = UUID.randomUUID(),
                    fom = 1.mai(2021),
                    tom = 31.mai(2021),
                    beløpTilbakekreves = tilbakekrevingsBeløp,
                    skattebeløp = skatt,
                    rentebeløp = renter,
                ),
            )
        }

        fun lagMmmelDto(
            alvorlighetsgrad: String,
            kodeMelding: String,
        ): MmelDto {
            val mmelDto = MmelDto()
            mmelDto.alvorlighetsgrad = alvorlighetsgrad
            mmelDto.kodeMelding = kodeMelding
            return mmelDto
        }

        fun readXml(fileName: String): String {
            val url = requireNotNull(this::class.java.getResource(fileName)) { "fil med filnavn=$fileName finnes ikke" }
            return url.readText()
        }
    }
}
