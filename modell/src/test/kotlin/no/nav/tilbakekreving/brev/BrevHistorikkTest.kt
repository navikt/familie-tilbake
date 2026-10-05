package no.nav.tilbakekreving.brev

import io.kotest.inspectors.forSingle
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.HistorikkStub.Companion.fakeReferanse
import no.nav.tilbakekreving.KlokkeStub
import no.nav.tilbakekreving.defaultFeatures
import no.nav.tilbakekreving.kontrakter.frontend.models.DokumentInfoDto
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.test.FellesTestdata.SAKSBEHANDLER_IDENT
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test

class BrevHistorikkTest {
    @Test
    fun `tom brevhistorikk`() {
        BrevHistorikk(mutableListOf()).alleSendteDokumenter() shouldBe emptyList()
    }

    @Test
    fun `sendte varselbrev og vedtaksbrev i samme historikk`() {
        val klokke = KlokkeStub(10.januar(2025))
        val varselbrev = Varselbrev.opprett(
            ansvarligSaksbehandlerIdent = SAKSBEHANDLER_IDENT,
            kravgrunnlag = fakeReferanse(kravgrunnlag()),
            varseltekstFraSaksbehandler = "",
            features = defaultFeatures(),
            klokke = klokke,
        ).apply { brevSendt("journalpost-varsel", "dokument-varsel") }
        klokke.settTid(11.januar(2025))
        val vedtaksbrev = Vedtaksbrev.opprett(klokke).apply {
            brevSendt("journalpost-vedtak", "dokument-vedtak")
        }
        val historikk = BrevHistorikk(mutableListOf())
        historikk.lagre(varselbrev)
        historikk.lagre(Vedtaksbrev.opprett(klokke))
        historikk.lagre(vedtaksbrev)

        val dokumenter = historikk.alleSendteDokumenter()

        dokumenter shouldBe listOf(
            DokumentInfoDto(
                brevSendt = 10.januar(2025),
                journalpostId = "journalpost-varsel",
                dokumentId = "dokument-varsel",
            ),
            DokumentInfoDto(
                brevSendt = 11.januar(2025),
                journalpostId = "journalpost-vedtak",
                dokumentId = "dokument-vedtak",
            ),
        )
    }

    @Test
    fun `brev som mangler begge dokumentreferansene`() {
        val historikk = BrevHistorikk(mutableListOf())
        historikk.lagre(Vedtaksbrev.opprett(KlokkeStub(10.januar(2025))))

        historikk.alleSendteDokumenter() shouldBe emptyList()
    }

    @Test
    fun `brev som bare har dokumentInfoId`() {
        val brev = Vedtaksbrev.opprett(KlokkeStub(10.januar(2025))).copy(
            dokumentInfoId = "dokument",
        )
        val historikk = BrevHistorikk(mutableListOf())
        historikk.lagre(brev)

        historikk.alleSendteDokumenter() shouldBe emptyList()
    }

    @Test
    fun `brev som bare har journalpostId`() {
        val brev = Vedtaksbrev.opprett(KlokkeStub(10.januar(2025))).copy(
            journalpostId = "journalpost",
        )
        val historikk = BrevHistorikk(mutableListOf())
        historikk.lagre(brev)

        historikk.alleSendteDokumenter() shouldBe emptyList()
    }

    @Test
    fun `brev sendes etter at det er lagt til i historikken`() {
        val klokke = KlokkeStub(10.januar(2025))
        val brev = Vedtaksbrev.opprett(klokke)
        val historikk = BrevHistorikk(mutableListOf())
        historikk.lagre(brev)
        historikk.alleSendteDokumenter() shouldBe emptyList()

        brev.brevSendt("journalpost", "dokument")

        historikk.alleSendteDokumenter().forSingle {
            it.journalpostId shouldBe "journalpost"
            it.dokumentId shouldBe "dokument"
            it.brevSendt shouldBe klokke.dagensDato()
        }
    }
}
