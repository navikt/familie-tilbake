package no.nav.familie.tilbake.common.exceptionhandler

import io.kotest.matchers.shouldBe
import no.nav.tilbakekreving.feil.ModellFeil
import no.nav.tilbakekreving.feil.Sporing
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class ApiExceptionHandlerTest {
    @Test
    fun `tjeneste utilgjengelig returnerer 503 med vedlikeholdstekst`() {
        val feil = ModellFeil.TjenesteUtilgjengeligException(Sporing("fagsak", "behandling"))

        val respons = ApiExceptionHandler().handleThrowable(feil)

        respons.statusCode shouldBe HttpStatus.SERVICE_UNAVAILABLE
        respons.body?.tittel shouldBe "Fryseperiode 9. oktober kl. 16:00–19. oktober kl. 08:00"
        respons.body?.melding shouldBe "Skatteetaten avvikler PAK og migrerer til Innfri. I denne perioden er det ikke mulig å sende vedtak til beslutter i Tilbakeløsningen."
    }
}
