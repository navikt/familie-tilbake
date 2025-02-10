package no.nav.familie.tilbake.config

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ConstantsTest {
    @Test
    fun `Henter ut riktig rettsgebyr for 2022`() {
        val rettsgebyr2022 = 1223
        Constants.rettsgebyrForÅr(2022) shouldBe rettsgebyr2022
    }

    @Test
    fun `Rettsgebyr framover i tid skal være samme som nå`() {
        Constants.rettsgebyrForÅr(LocalDate.now().plusYears(3).year) shouldBe Constants.rettsgebyrForÅr(LocalDate.now().year)
    }

    @Test
    fun `Henter ikke ut rettsgebyr for 2020 - ikke registrert`() {
        assertNull(Constants.rettsgebyrForÅr(2020))
    }
}
