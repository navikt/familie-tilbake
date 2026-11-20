package no.nav.tilbakekreving.brev.mottaker

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PoststedTest {
    @Test
    fun `leser postnummer-fil`() {
        Poststed.alle["2260"] shouldBe "KIRKENÆR"
        Poststed.alle["0661"] shouldBe "OSLO"
    }
}
