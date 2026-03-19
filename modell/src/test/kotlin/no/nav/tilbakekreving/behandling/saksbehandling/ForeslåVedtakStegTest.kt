package no.nav.tilbakekreving.behandling.saksbehandling

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.UUID

class ForeslåVedtakStegTest {
    @Test
    fun `kan sende vedtak til godkjenning`() {
        val foreslåVedtakSteg = ForeslåVedtakSteg.opprett()
        foreslåVedtakSteg.erFullstendig() shouldBe false

        foreslåVedtakSteg.håndter()
        foreslåVedtakSteg.erFullstendig() shouldBe true
    }

    @Test
    fun `underkjenning blir lagret`() {
        val foreslåVedtakSteg = ForeslåVedtakSteg.opprett()

        foreslåVedtakSteg.underkjennSteget()

        foreslåVedtakSteg.tilEntity(UUID.randomUUID()).trengerNyVurdering shouldBe true
    }
}
