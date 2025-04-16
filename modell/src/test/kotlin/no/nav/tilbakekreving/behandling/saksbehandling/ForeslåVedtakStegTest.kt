package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ForeslåVedtakStegTest {
    @Test
    fun `kan sende vedtak til godkjenning`() {
        val foreslåVedtakSteg = ForeslåVedtakSteg.opprett()
        foreslåVedtakSteg.erFullstending() shouldBe false

        foreslåVedtakSteg.håndter(
            ForeslåVedtakSteg.Vurdering.ForeslåVedtak(
                oppsummeringstekst = null,
                perioderMedTekst = emptyList(),
            ),
        )
        foreslåVedtakSteg.erFullstending() shouldBe true
    }
}
