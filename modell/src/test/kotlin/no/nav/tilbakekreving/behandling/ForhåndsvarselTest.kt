package no.nav.tilbakekreving.behandling

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class ForhåndsvarselTest {
    @Test
    fun `skal gi melding til saksbehandler dersom bruker har uttalt seg på forhåndsvarsel`() {
        val forhåndsvarsel = Forhåndsvarsel.opprett(LocalDate.now().minusDays(1))
        forhåndsvarsel.lagreUttalelse(
            uttalelseVurdering = UttalelseVurdering.JA_ETTER_FORHÅNDSVARSEL,
            uttalelseInfo = listOf(
                UttalelseInfo(
                    id = UUID.randomUUID(),
                    uttalelsesdato = LocalDate.now(),
                    hvorBrukerenUttalteSeg = "Reddit",
                    uttalelseBeskrivelse = "Typisk reddit kommentar",
                ),
            ),
            kommentar = "Vet ærlig talt ikke hva hen prater om.",
        )

        forhåndsvarsel.meldingerTilSaksbehandler() shouldBe setOf(MeldingTilSaksbehandler.BEGRUNN_BRUKERS_UTTALELSE)
    }

    @Test
    fun `underkjenning blir lagret`() {
        val forhåndsvarsel = Forhåndsvarsel.opprett(null)

        forhåndsvarsel.lagreForhåndsvarselUnntak(
            begrunnelseForUnntak = BegrunnelseForUnntak.ALLEREDE_UTTALET_SEG,
            beskrivelse = "",
        )
        forhåndsvarsel.lagreUttalelse(
            UttalelseVurdering.UNNTAK_ALLEREDE_UTTALT_SEG,
            uttalelseInfo = emptyList(),
            kommentar = "",
        )
        forhåndsvarsel.underkjennSteget()

        val forhåndsvarselEntity = forhåndsvarsel.tilEntity(UUID.randomUUID())
        forhåndsvarselEntity.forhåndsvarselUnntakEntity?.trengerNyVurdering shouldBe true
        forhåndsvarselEntity.brukeruttalelseEntity?.trengerNyVurdering shouldBe true
    }
}
