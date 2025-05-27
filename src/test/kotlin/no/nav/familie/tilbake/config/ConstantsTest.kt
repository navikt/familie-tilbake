package no.nav.familie.tilbake.config

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.Rettsgebyr
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ConstantsTest {
    @Test
    fun `Henter ut riktig rettsgebyr for 2022`() {
        val rettsgebyr2022 = 1223
        Rettsgebyr.rettsgebyrForÅr(2022) shouldBe rettsgebyr2022
    }

    @Test
    fun `Rettsgebyr framover i tid skal være samme som nå`() {
        Rettsgebyr.rettsgebyrForÅr(LocalDate.now().plusYears(3).year) shouldBe Rettsgebyr.rettsgebyrForÅr(LocalDate.now().year)
    }

    @Test
    fun `Henter ikke ut rettsgebyr for 2020 - ikke registrert`() {
        assertNull(Rettsgebyr.rettsgebyrForÅr(2020))
    }
}
