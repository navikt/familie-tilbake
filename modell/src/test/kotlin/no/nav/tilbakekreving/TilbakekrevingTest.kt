package no.nav.tilbakekreving
import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.api.v2.Opprettelsesvalg
import no.nav.tilbakekreving.tilstand.AvventerKravgrunnlag
import org.junit.jupiter.api.Test
import java.util.UUID

class TilbakekrevingTest {
    @Test
    fun `oppretter tilbakekreving`() {
        val opprettEvent = opprettTilbakekrevingHendelse(opprettelsesvalg = Opprettelsesvalg.OPPRETT_TILBAKEKREVING_UTEN_VARSEL)
        val tilbakekreving = Tilbakekreving.opprett(
            id = UUID.randomUUID().toString(),
            opprettTilbakekrevingEvent = opprettEvent,
            sideeffektContext = systemContext(),
        )

        tilbakekreving.tilstand shouldBe AvventerKravgrunnlag
    }
}
