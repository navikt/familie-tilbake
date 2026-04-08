package no.nav.tilbakekreving.brev.vedtaksbrev

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class BrevFormattererTest {
    @Test
    fun `overskrift baserer seg på utfall på saken`() {
        BrevFormatterer.lagHovedavsnittTittel(vedtaksbrevInfo(skalTilbakerkeves = true)) shouldBe "Du må betale tilbake arbeidsavklaringspenger"
        BrevFormatterer.lagHovedavsnittTittel(vedtaksbrevInfo(skalTilbakerkeves = false)) shouldBe "Du må ikke betale tilbake arbeidsavklaringspenger"
    }
}
