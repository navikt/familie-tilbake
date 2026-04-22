package no.nav.tilbakekreving.brev.vedtaksbrev

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.breeeev.standardtekster.HjemmelForTilbakekreving
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import org.junit.jupiter.api.Test

class BrevFormattererTest {
    @Test
    fun `overskrift baserer seg på utfall på saken`() {
        BrevFormatterer.lagHovedavsnittTittel(vedtaksbrevInfo(skalTilbakerkeves = true)) shouldBe "Du må betale tilbake arbeidsavklaringspenger"
        BrevFormatterer.lagHovedavsnittTittel(vedtaksbrevInfo(skalTilbakerkeves = false)) shouldBe "Du må ikke betale tilbake arbeidsavklaringspenger"
    }

    @Test
    fun `avsnitt for hjemmel for tilbakekreving`() {
        BrevFormatterer.lagVedtakHjemmelAvsnitt(
            listOf(
                HjemmelForTilbakekreving.BARNETRYGDLOVEN_13,
                HjemmelForTilbakekreving.FOLKETRYGDLOVEN_22_15,
            ),
            Språkkode.NB,
        ) shouldBe "Vedtaket er gjort etter barnetrygdloven § 13 og folketrygdloven § 22-15."
    }

    @Test
    fun `formattering av beløp`() {
        BrevFormatterer.beløpString(6900) shouldBe "6 900"
        BrevFormatterer.beløpString(69000) shouldBe "69 000"
    }
}
