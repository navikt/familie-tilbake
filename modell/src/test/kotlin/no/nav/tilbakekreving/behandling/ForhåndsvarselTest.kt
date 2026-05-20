package no.nav.tilbakekreving.behandling

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.SystemKlokke
import no.nav.tilbakekreving.behandling.saksbehandling.Venter
import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler
import org.junit.jupiter.api.Test
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
        forhåndsvarselEntity.forhåndsvarselUnntakEntity?.trengerNyVurdering shouldBe true
        forhåndsvarselEntity.brukeruttalelseEntity?.trengerNyVurdering shouldBe true
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
}
