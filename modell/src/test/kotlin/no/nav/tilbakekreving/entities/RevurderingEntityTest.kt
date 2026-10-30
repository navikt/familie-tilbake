package no.nav.tilbakekreving.entities

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.behandling.TilbakekrevingRevurderingsårsak
import no.nav.tilbakekreving.behandling.TilbakekrevingRevurderingsårsakType
import no.nav.tilbakekreving.behandling.VedtakOmgjortAv
import org.junit.jupiter.api.Test
import java.util.UUID

class RevurderingEntityTest {
    @Test
    fun `Revurdering av tilbakekrevingen blir lagret`() {
        val tilbakekrevingRevurderingFørLagring = TilbakekrevingRevurderingsårsak.EtterKlage(
            årsakType = TilbakekrevingRevurderingsårsakType.ETTER_KLAGE,
            vedtakOmgjortAv = VedtakOmgjortAv.Klageinstansen,
            beskrivelse = "Dette er en test",
        )

        val tilbakekrevingRevurderingEtterLagring = tilbakekrevingRevurderingFørLagring.tilEntity(UUID.randomUUID()).fraEntity()

        tilbakekrevingRevurderingEtterLagring shouldBe tilbakekrevingRevurderingFørLagring
    }
}
