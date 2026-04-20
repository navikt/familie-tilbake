package no.nav.tilbakekreving.brev.vedtaksbrev

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.breeeev.standardtekster.HjemmelForTilbakekreving
import org.junit.jupiter.api.Test

class BrevFormattererTest {
    @Test
    fun `overskrift baserer seg på utfall på saken`() {
        BrevFormatterer.lagHovedavsnittTittel(vedtaksbrevInfo(skalTilbakerkeves = true)) shouldBe "Du må betale tilbake arbeidsavklaringspenger"
        BrevFormatterer.lagHovedavsnittTittel(vedtaksbrevInfo(skalTilbakerkeves = false)) shouldBe "Du må ikke betale tilbake arbeidsavklaringspenger"
    }

    @Test
    fun `avsnitt for hjemmel for tilbakekreving`() {
        BrevFormatterer.lagHjemmelAvsnitt(
            listOf(
                HjemmelForTilbakekreving.BARNETRYGDLOVEN_13,
                HjemmelForTilbakekreving.FOLKETRYGDLOVEN_22_15,
            ),
        ) shouldBe "Vedtaket er gjort etter barnetrygdloven § 13 samt folketrygdloven § 22-15"
    }
}
