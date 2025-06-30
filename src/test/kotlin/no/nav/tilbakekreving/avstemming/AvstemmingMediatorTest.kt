package no.nav.tilbakekreving.avstemming

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.e2e.E2EBase
import no.nav.tilbakekreving.entities.AktørEntity
import no.nav.tilbakekreving.entities.AktørType
import no.nav.tilbakekreving.fagsystem.Ytelsestype
import no.nav.tilbakekreving.januar
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.mai
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsbelopDto
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsperiodeDto
import no.nav.tilbakekreving.typer.v1.PeriodeDto
import no.nav.tilbakekreving.vedtak.IverksattVedtak
import no.nav.tilbakekreving.vedtak.IverksettRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class AvstemmingMediatorTest : E2EBase() {
    @Autowired
    lateinit var avstemmingMediator: AvstemmingMediator

    @Autowired
    lateinit var iverksettRepository: IverksettRepository

    @Test
    fun `test avstemming`() {
        val opprettetTid = LocalDate.now()

        val iverksattVedtak1 = IverksattVedtak(
            id = UUID.fromString("11111111-2222-3333-4444-55555555555a"),
            behandlingId = UUID.fromString("11111111-2222-3333-4444-55555555555b"),
            vedtakId = BigInteger("1234"),
            aktør = AktørEntity(aktørType = AktørType.Person, "11223344556"),
            opprettetTid = opprettetTid,
            ytelsestype = Ytelsestype.TILLEGGSSTØNADER,
            kvittering = "04",
            tilbakekrevingsperioder = listOf(
                TilbakekrevingsperiodeDto()
                    .apply {
                        periode = PeriodeDto().apply {
                            this.fom = 1.januar(2021)
                            this.tom = 31.januar(2021)
                        }
                        renterBeregnes = "renterBeregnes"
                        belopRenter = BigDecimal("100.00")
                        tilbakekrevingsbelop.add(
                            TilbakekrevingsbelopDto().apply {
                                kodeKlasse = "klasse"
                                belopOpprUtbet = BigDecimal("123.00")
                                belopNy = BigDecimal("1230.00")
                                belopTilbakekreves = BigDecimal("3000.00")
                                belopUinnkrevd = BigDecimal("3.00")
                                belopSkatt = BigDecimal("3.00")
                                kodeResultat = "Kode resultat"
                                kodeAarsak = "Kode årsak"
                                kodeSkyld = "kode skyld"
                            },
                        )
                    },
                TilbakekrevingsperiodeDto()
                    .apply {
                        periode = PeriodeDto().apply {
                            this.fom = 1.mai(2021)
                            this.tom = 31.mai(2021)
                        }
                        renterBeregnes = "renterBeregnes2"
                        belopRenter = BigDecimal("100.00")
                        tilbakekrevingsbelop.add(
                            TilbakekrevingsbelopDto().apply {
                                kodeKlasse = "klasse2"
                                belopOpprUtbet = BigDecimal("123.00")
                                belopNy = BigDecimal("1230.00")
                                belopTilbakekreves = BigDecimal("3000.00")
                                belopUinnkrevd = BigDecimal("3.00")
                                belopSkatt = BigDecimal("3.00")
                                kodeResultat = "Kode resultat2"
                                kodeAarsak = "Kode årsak2"
                                kodeSkyld = "kode skyld2"
                            },
                        )
                    },
            ),
            behandlingstype = Behandlingstype.TILBAKEKREVING,
        )

        val iverksattVedtak2 = IverksattVedtak(
            id = UUID.fromString("11111111-2222-3333-4444-5555555555aa"),
            behandlingId = UUID.fromString("11111111-2222-3333-4444-5555555555bb"),
            vedtakId = BigInteger("1235"),
            aktør = AktørEntity(aktørType = AktørType.Person, "11223344557"),
            opprettetTid = opprettetTid,
            ytelsestype = Ytelsestype.TILLEGGSSTØNADER,
            kvittering = "00",
            tilbakekrevingsperioder = listOf(
                TilbakekrevingsperiodeDto()
                    .apply {
                        periode = PeriodeDto().apply {
                            this.fom = 1.januar(2021)
                            this.tom = 31.januar(2021)
                        }
                        renterBeregnes = "renterBeregnes"
                        belopRenter = BigDecimal("50.00")
                        tilbakekrevingsbelop.add(
                            TilbakekrevingsbelopDto().apply {
                                kodeKlasse = "klasse"
                                belopOpprUtbet = BigDecimal("123.00")
                                belopNy = BigDecimal("1230.00")
                                belopTilbakekreves = BigDecimal("0.00")
                                belopUinnkrevd = BigDecimal("3.00")
                                belopSkatt = BigDecimal("0.00")
                                kodeResultat = "Kode resultat"
                                kodeAarsak = "Kode årsak"
                                kodeSkyld = "kode skyld"
                            },
                        )
                    },
                TilbakekrevingsperiodeDto()
                    .apply {
                        periode = PeriodeDto().apply {
                            this.fom = 1.mai(2021)
                            this.tom = 31.mai(2021)
                        }
                        renterBeregnes = "renterBeregnes2"
                        belopRenter = BigDecimal("50.00")
                        tilbakekrevingsbelop.add(
                            TilbakekrevingsbelopDto().apply {
                                kodeKlasse = "klasse2"
                                belopOpprUtbet = BigDecimal("123.00")
                                belopNy = BigDecimal("1230.00")
                                belopTilbakekreves = BigDecimal("0.00")
                                belopUinnkrevd = BigDecimal("3.00")
                                belopSkatt = BigDecimal("0.00")
                                kodeResultat = "Kode resultat2"
                                kodeAarsak = "Kode årsak2"
                                kodeSkyld = "kode skyld2"
                            },
                        )
                    },
            ),
            behandlingstype = Behandlingstype.REVURDERING_TILBAKEKREVING,
        )

        iverksettRepository.lagreIverksattVedtak(iverksattVedtak = iverksattVedtak1)
        iverksettRepository.lagreIverksattVedtak(iverksattVedtak = iverksattVedtak2)

        val result = avstemmingMediator.avstem(dato = opprettetTid)

        val expected = """
            avsender;vedtakId;fnr;vedtaksdato;fagsakYtelseType;tilbakekrevesBruttoUtenRenter;skatt;tilbakekrevesNettoUtenRenter;renter;erOmgjøringTilIngenTilbakekreving
            familie-tilbake;1234;11223344556;${opprettetTid.format(DateTimeFormatter.ofPattern("yyyyMMdd"))};${Ytelsestype.TILLEGGSSTØNADER.kode};6000;6;5994;200;
            familie-tilbake;1235;11223344557;${opprettetTid.format(DateTimeFormatter.ofPattern("yyyyMMdd"))};${Ytelsestype.TILLEGGSSTØNADER.kode};0;0;0;100;Omgjoring0
        """.trimIndent().trimEnd()

        result?.toString(Charsets.UTF_8) shouldBe expected
    }
}
