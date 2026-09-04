package no.nav.tilbakekreving.behandling

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.SystemKlokke
import no.nav.tilbakekreving.behandling.saksbehandling.Venter
import no.nav.tilbakekreving.behandling.saksbehandling.ÅrsakTilTilbakeføring
import no.nav.tilbakekreving.beregning.BeregningTest.TestKravgrunnlagPeriode.Companion.kroner
import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler
import no.nav.tilbakekreving.kontrakter.periode.til
import no.nav.tilbakekreving.kravgrunnlag.KravgrunnlagSammenligning
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
    fun `deler av behandling påbegynt`() {
        val forhåndsvarsel = Forhåndsvarsel.opprett()

        forhåndsvarsel.lagreOpprinneligFrist(LocalDate.now())
        forhåndsvarsel.erPåbegynt() shouldBe true
    }

    @Test
    fun `ny periode i kravgrunnlag krever ny vurdering av forhåndsvarselunntak`() {
        val forhåndsvarsel = Forhåndsvarsel.opprett()
        forhåndsvarsel.lagreForhåndsvarselUnntak(
            begrunnelseForUnntak = BegrunnelseForUnntak.ALLEREDE_UTTALET_SEG,
            beskrivelse = "",
        )

        forhåndsvarsel.nyPeriode(KravgrunnlagSammenligning.Forskjell.NyPeriode(1.februar(2021) til 28.februar(2021), BigDecimal("2000")))

        val forhåndsvarselEntity = forhåndsvarsel.tilEntity(UUID.randomUUID())
        forhåndsvarselEntity.forhåndsvarselUnntakEntity?.tilbakeført shouldBe ÅrsakTilTilbakeføring.NyttKravgrunnlag
    }

    @Test
    fun `endret periode i kravgrunnlag krever ny vurdering av forhåndsvarselunntak`() {
        val forhåndsvarsel = Forhåndsvarsel.opprett()
        forhåndsvarsel.lagreForhåndsvarselUnntak(
            begrunnelseForUnntak = BegrunnelseForUnntak.ALLEREDE_UTTALET_SEG,
            beskrivelse = "",
        )

        forhåndsvarsel.periodeEndret(
            KravgrunnlagSammenligning.Forskjell.EndretPeriode(
                periode = 1.januar(2021) til 31.januar(2021),
                nyPeriode = 1.januar(2021) til 20.januar(2021),
                gammeltBeløp = 1000.kroner,
                nyttBeløp = 1500.kroner,
                etterfølgende = null,
            ),
        )

        val forhåndsvarselEntity = forhåndsvarsel.tilEntity(UUID.randomUUID())
        forhåndsvarselEntity.forhåndsvarselUnntakEntity?.tilbakeført shouldBe ÅrsakTilTilbakeføring.NyttKravgrunnlag
    }
}
