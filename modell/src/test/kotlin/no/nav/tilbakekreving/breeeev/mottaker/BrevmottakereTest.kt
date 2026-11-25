package no.nav.tilbakekreving.breeeev.mottaker

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.kontrakter.frontend.models.FregAdresseDto
import no.nav.tilbakekreving.kontrakter.frontend.models.NorskAdresseDto
import no.nav.tilbakekreving.systemContext
import org.junit.jupiter.api.Test
import java.util.UUID

class BrevmottakereTest {
    @Test
    fun `bytter til manuelt satt adresse`() {
        val brevmottakere = Brevmottakere.opprett()

        brevmottakere.oppdaterBrevmottaker(
            sideeffektContext = systemContext(),
            hovedmottaker = Addresse.Norsk(UUID.randomUUID(), "Testveien 1", "0123"),
        ) shouldBe NorskAdresseDto(gate = "Testveien 1", postnummer = "0123")
        brevmottakere.tilFrontendDto().hovedmottaker shouldBe NorskAdresseDto(gate = "Testveien 1", postnummer = "0123")
    }

    @Test
    fun `tilbake til folkeregistrert adresse fra norsk adresse`() {
        val brevmottakere = Brevmottakere.opprett()
        brevmottakere.oppdaterBrevmottaker(
            sideeffektContext = systemContext(),
            hovedmottaker = Addresse.Norsk(UUID.randomUUID(), "Testveien 1", "0123"),
        )

        brevmottakere.oppdaterBrevmottaker(
            sideeffektContext = systemContext(),
            hovedmottaker = Hovedmottaker.FolkeregistrertAddresse,
        ) shouldBe FregAdresseDto
        brevmottakere.tilFrontendDto().hovedmottaker shouldBe FregAdresseDto
    }
}
