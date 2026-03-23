package no.nav.tilbakekreving.tekst

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TextUtilTest {
    @Test
    fun a() {
        listOf("a").slåSammen() shouldBe "a"
    }

    @Test
    fun `a og b`() {
        listOf("a", "b").slåSammen() shouldBe "a og b"
    }

    @Test
    fun `a, b og c`() {
        listOf("a", "b", "c").slåSammen() shouldBe "a, b og c"
    }
}
