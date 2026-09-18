package no.nav.tilbakekreving.behandling

import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import no.nav.tilbakekreving.HistorikkStub.Companion.fakeReferanse
import no.nav.tilbakekreving.SystemKlokke
import no.nav.tilbakekreving.behandling.saksbehandling.BehandlingsstatusModell
import no.nav.tilbakekreving.behandling.saksbehandling.Venter
import no.nav.tilbakekreving.behandling.saksbehandling.ÅrsakTilTilbakeføring
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler
import no.nav.tilbakekreving.brev.Varselbrev
import no.nav.tilbakekreving.defaultFeatures
import no.nav.tilbakekreving.kontrakter.frontend.models.ArsakTilTilbakeforingDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForhaandsvarselErSendtDto
import no.nav.tilbakekreving.kontrakter.frontend.models.ForhaandsvarselUnntakDto
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

        val forhåndsvarselEntity = forhåndsvarsel.tilEntity(UUID.randomUUID())
        forhåndsvarselEntity.forhåndsvarselUnntakEntity?.tilbakeført shouldBe ÅrsakTilTilbakeføring.Underkjent
        forhåndsvarselEntity.brukeruttalelseEntity?.tilbakeført shouldBe ÅrsakTilTilbakeføring.Underkjent
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
        val varselbrev = Varselbrev.opprett(
            "",
            fakeReferanse(kravgrunnlag()),
            "",
            defaultFeatures(),
            SystemKlokke,
        )
        val forhåndsvarsel = Forhåndsvarsel.opprett()
        forhåndsvarsel.lagreForhåndsvarselUnntak(
            begrunnelseForUnntak = BegrunnelseForUnntak.IKKE_PRAKTISK_MULIG,
            beskrivelse = "",
        )
        forhåndsvarsel.erForhåndsvarselSendt() shouldBe false

        forhåndsvarsel.lagreOpprinneligFrist(LocalDate.now())

        forhåndsvarsel.erForhåndsvarselSendt() shouldBe true
        forhåndsvarsel.nyForhåndsvarselTilFrontend(varselbrev).forhaandsvarselSteg.shouldBeInstanceOf<ForhaandsvarselErSendtDto>()
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

        forhåndsvarsel.nyForhåndsvarselTilFrontend(null)
            .forhaandsvarselSteg.shouldBeInstanceOf<ForhaandsvarselUnntakDto>()
            .tilbakeført shouldBe ArsakTilTilbakeforingDto.NyttKravgrunnlag
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

        forhåndsvarsel.nyForhåndsvarselTilFrontend(null)
            .forhaandsvarselSteg.shouldBeInstanceOf<ForhaandsvarselUnntakDto>()
            .tilbakeført shouldBe null
    }
}
