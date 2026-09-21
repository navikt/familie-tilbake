package no.nav.tilbakekreving.behandling

import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.HistorikkStub.Companion.fakeReferanse
import no.nav.tilbakekreving.KlokkeStub
import no.nav.tilbakekreving.SystemKlokke
import no.nav.tilbakekreving.behandling.saksbehandling.BehandlingsstatusModell
import no.nav.tilbakekreving.behandling.saksbehandling.Venter
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler
import no.nav.tilbakekreving.brev.Varselbrev
import no.nav.tilbakekreving.defaultFeatures
import no.nav.tilbakekreving.kontrakter.frontend.models.ArsakTilTilbakeforingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForhaandsvarselErSendtDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForhaandsvarselUnntakDto
import no.nav.tilbakekreving.kontrakter.frontend.models.IkkeVurdertDto
import no.nav.tilbakekreving.kravgrunnlag
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagSammenligning.OverordnetSammendrag
import no.nav.tilbakekreving.test.februar
import no.nav.tilbakekreving.test.januar
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class ForhåndsvarselTest {
    @Test
    fun `skal gi melding til saksbehandler dersom bruker har uttalt seg på forhåndsvarsel`() {
        val forhåndsvarsel = Forhåndsvarsel.opprett()
        forhåndsvarsel.lagreOpprinneligFrist(LocalDate.now())
        forhåndsvarsel.lagreUttalelse(
            uttalelseVurdering = UttalelseVurdering.JA_ETTER_FORHÅNDSVARSEL,
            uttalelseInfo = UttalelseInfo(
                id = UUID.randomUUID(),
                uttalelsesdato = LocalDate.now(),
                hvorBrukerenUttalteSeg = "Reddit",
                uttalelseBeskrivelse = "Typisk reddit kommentar",
            ),
            kommentar = null,
        )

        forhåndsvarsel.meldingerTilSaksbehandler() shouldBe setOf(MeldingTilSaksbehandler.BEGRUNN_BRUKERS_UTTALELSE)
    }

    @Test
    fun `underkjenning blir lagret`() {
        val forhåndsvarsel = Forhåndsvarsel.opprett()

        forhåndsvarsel.lagreForhåndsvarselUnntak(
            begrunnelseForUnntak = BegrunnelseForUnntak.ALLEREDE_UTTALET_SEG,
            beskrivelse = "",
        )
        forhåndsvarsel.lagreUttalelse(
            UttalelseVurdering.UNNTAK_ALLEREDE_UTTALT_SEG,
            uttalelseInfo = UttalelseInfo(
                id = UUID.randomUUID(),
                uttalelsesdato = LocalDate.now(),
                hvorBrukerenUttalteSeg = "Reddit",
                uttalelseBeskrivelse = "Typisk reddit kommentar",
            ),
            kommentar = null,
        )
        forhåndsvarsel.underkjennSteget()

        forhåndsvarsel.nyForhåndsvarselTilFrontend(varselbrev(), SystemKlokke).tilbakeført shouldBe ArsakTilTilbakeforingDto.TilbakemeldingFraSaksbehandler
    }

    @Test
    fun `utsettelse av uttalse, fristen er utgått`() {
        val forhåndsvarsel = Forhåndsvarsel.opprett()
        forhåndsvarsel.lagreOpprinneligFrist(LocalDate.now().minusDays(1))

        forhåndsvarsel.venter(SystemKlokke) shouldBe null
    }

    @Test
    fun `utsettelse av uttalse, fristen er i fremtiden`() {
        val uttalelsesfrist = LocalDate.now().plusDays(1)
        val forhåndsvarsel = Forhåndsvarsel.opprett()
        forhåndsvarsel.lagreOpprinneligFrist(uttalelsesfrist)

        forhåndsvarsel.venter(SystemKlokke) shouldBe Venter(
            grunn = Venter.Grunn.BRUKERUTTALELSE,
            frist = uttalelsesfrist,
        )
    }

    @Test
    fun `ikke påbegynt`() {
        val forhåndsvarsel = Forhåndsvarsel.opprett()

        forhåndsvarsel.erPåbegynt() shouldBe false
    }

    @Test
    fun `forhåndsvarsel sendes`() {
        val forhåndsvarsel = Forhåndsvarsel.opprett()
        forhåndsvarsel.erForhåndsvarselSendt() shouldBe null
        forhåndsvarsel.behandlingsstatus shouldBe BehandlingsstatusModell.TIL_FORHÅNDSVARSEL

        forhåndsvarsel.lagreOpprinneligFrist(LocalDate.now())

        forhåndsvarsel.erForhåndsvarselSendt() shouldBe true
        forhåndsvarsel.behandlingsstatus shouldBe BehandlingsstatusModell.TIL_BEHANDLING
        forhåndsvarsel.erPåbegynt() shouldBe true
    }

    @Test
    fun `forhåndsvarsel sendes etter tidligere unntak var registrert`() {
        val forhåndsvarsel = Forhåndsvarsel.opprett()
        forhåndsvarsel.lagreForhåndsvarselUnntak(
            begrunnelseForUnntak = BegrunnelseForUnntak.IKKE_PRAKTISK_MULIG,
            beskrivelse = "",
        )
        forhåndsvarsel.erForhåndsvarselSendt() shouldBe false

        forhåndsvarsel.lagreOpprinneligFrist(LocalDate.now())

        forhåndsvarsel.erForhåndsvarselSendt() shouldBe true
        forhåndsvarsel.nyForhåndsvarselTilFrontend(varselbrev(), SystemKlokke).forhaandsvarselSteg.shouldBeInstanceOf<ForhaandsvarselErSendtDto>()
    }

    @Test
    fun `økt beløp i kravgrunnlaget fører til ny vurdering av forhåndsvarselunntak`() {
        val forhåndsvarsel = Forhåndsvarsel.opprett()
        forhåndsvarsel.lagreForhåndsvarselUnntak(
            begrunnelseForUnntak = BegrunnelseForUnntak.ALLEREDE_UTTALET_SEG,
            beskrivelse = "",
        )

        forhåndsvarsel.nyttKravgrunnlagMottatt(
            OverordnetSammendrag(
                fom = 1.januar(2021),
                tom = 31.januar(2021),
                nyttBeløp = 1500.kroner,
                gammeltBeløp = 1000.kroner,
            ),
        )

        forhåndsvarsel.nyForhåndsvarselTilFrontend(null, SystemKlokke).should {
            it.forhaandsvarselSteg.shouldBeInstanceOf<ForhaandsvarselUnntakDto>()
            it.tilbakeført shouldBe ArsakTilTilbakeforingDto.NyttKravgrunnlag
        }
    }

    @Test
    fun `redusert beløp i kravgrunnlaget fører ikke til ny vurdering av forhåndsvarselunntak`() {
        val forhåndsvarsel = Forhåndsvarsel.opprett()
        forhåndsvarsel.lagreForhåndsvarselUnntak(
            begrunnelseForUnntak = BegrunnelseForUnntak.ALLEREDE_UTTALET_SEG,
            beskrivelse = "",
        )

        forhåndsvarsel.nyttKravgrunnlagMottatt(
            OverordnetSammendrag(
                fom = 1.februar(2021),
                tom = 28.februar(2021),
                nyttBeløp = BigDecimal.ZERO,
                gammeltBeløp = 2000.kroner,
            ),
        )

        forhåndsvarsel.nyForhåndsvarselTilFrontend(null, SystemKlokke).should {
            it.forhaandsvarselSteg.shouldBeInstanceOf<ForhaandsvarselUnntakDto>()
            it.tilbakeført shouldBe null
        }
    }

    @Test
    fun `nytt kravgrunnlag etter sendt forhåndsvarsel - må vurderes på nytt`() {
        val forhåndsvarsel = forhåndsvarselSendtMedUttalelse("Brukeren har uttalt seg")

        forhåndsvarsel.nyttKravgrunnlagMottatt(øktBeløp())

        forhåndsvarsel.nyForhåndsvarselTilFrontend(varselbrev(), KlokkeStub(1.februar(2021))).should {
            it.forhaandsvarselSteg.shouldBeInstanceOf<IkkeVurdertDto>()
            it.brukeruttalelse?.beskrivelse shouldBe "Brukeren har uttalt seg"
            it.tilbakeført shouldBe ArsakTilTilbakeforingDto.NyttKravgrunnlag
        }
        forhåndsvarsel.erFullstendig(KlokkeStub(1.februar(2021))) shouldBe false
    }

    @Test
    fun `nytt kravgrunnlag etter sendt forhåndsvarsel - sender nytt forhåndsvarsel`() {
        val forhåndsvarsel = forhåndsvarselSendtMedUttalelse("Brukeren har uttalt seg")
        forhåndsvarsel.nyttKravgrunnlagMottatt(øktBeløp())

        forhåndsvarsel.lagreOpprinneligFrist(15.februar(2021))

        forhåndsvarsel.nyForhåndsvarselTilFrontend(varselbrev(), KlokkeStub(1.februar(2021))).should {
            it.forhaandsvarselSteg.shouldBeInstanceOf<ForhaandsvarselErSendtDto>()
            it.brukeruttalelse?.beskrivelse shouldBe "Brukeren har uttalt seg"
            it.tilbakeført shouldBe null
            it.ferdigvurdert shouldBe false
        }
        forhåndsvarsel.venter(KlokkeStub(1.februar(2021))) shouldBe Venter(grunn = Venter.Grunn.BRUKERUTTALELSE, frist = 15.februar(2021))
        forhåndsvarsel.venter(KlokkeStub(16.februar(2021))) shouldBe null
        forhåndsvarsel.erFullstendig(KlokkeStub(16.februar(2021))) shouldBe false
    }

    @Test
    fun `nytt kravgrunnlag etter sendt forhåndsvarsel - sender nytt forhåndsvarsel og endrer bruker uttalelse`() {
        val forhåndsvarsel = forhåndsvarselSendtMedUttalelse("Brukeren har uttalt seg")
        forhåndsvarsel.nyttKravgrunnlagMottatt(øktBeløp())

        forhåndsvarsel.lagreOpprinneligFrist(15.februar(2021))

        forhåndsvarsel.lagreUttalelse(UttalelseVurdering.JA_ETTER_FORHÅNDSVARSEL, uttalelse("Ny uttalelse"), "vurdering")

        forhåndsvarsel.nyForhåndsvarselTilFrontend(varselbrev(), KlokkeStub(1.februar(2021))).should {
            it.forhaandsvarselSteg.shouldBeInstanceOf<ForhaandsvarselErSendtDto>()
            it.brukeruttalelse?.beskrivelse shouldBe "Ny uttalelse"
            it.tilbakeført shouldBe null
            it.ferdigvurdert shouldBe true
        }
        forhåndsvarsel.erFullstendig(KlokkeStub(1.februar(2021))) shouldBe true
    }

    @Test
    fun `nytt kravgrunnlag etter sendt forhåndsvarsel - unntak for forhåndsvarsel`() {
        val forhåndsvarsel = forhåndsvarselSendtMedUttalelse("Brukeren har uttalt seg")
        forhåndsvarsel.nyttKravgrunnlagMottatt(øktBeløp())

        forhåndsvarsel.lagreForhåndsvarselUnntak(
            begrunnelseForUnntak = BegrunnelseForUnntak.ÅPENBART_UNØDVENDIG,
            beskrivelse = "Brukeren har uttalt seg",
        )

        forhåndsvarsel.nyForhåndsvarselTilFrontend(null, KlokkeStub(1.februar(2021))).should {
            it.forhaandsvarselSteg.shouldBeInstanceOf<ForhaandsvarselUnntakDto>()
            it.brukeruttalelse?.beskrivelse shouldBe "Brukeren har uttalt seg"
            it.tilbakeført shouldBe null
        }
        forhåndsvarsel.erFullstendig(KlokkeStub(1.februar(2021))) shouldBe true
    }

    private fun forhåndsvarselSendtMedUttalelse(uttalelse: String): Forhåndsvarsel = Forhåndsvarsel.opprett().also {
        it.lagreOpprinneligFrist(31.januar(2021))
        it.lagreUttalelse(
            uttalelseVurdering = UttalelseVurdering.JA_ETTER_FORHÅNDSVARSEL,
            uttalelseInfo = uttalelse(uttalelse),
            kommentar = "Uttalelsen er vurdert",
        )
    }

    fun uttalelse(uttalelse: String) = UttalelseInfo(
        id = UUID.randomUUID(),
        uttalelsesdato = 20.januar(2021),
        hvorBrukerenUttalteSeg = "Telefon",
        uttalelseBeskrivelse = uttalelse,
    )

    private fun øktBeløp() = OverordnetSammendrag(
        fom = 1.januar(2021),
        tom = 31.januar(2021),
        nyttBeløp = 1500.kroner,
        gammeltBeløp = 1000.kroner,
    )

    fun varselbrev() = Varselbrev.opprett(
        "",
        fakeReferanse(kravgrunnlag()),
        "",
        defaultFeatures(),
        SystemKlokke,
    )
}
