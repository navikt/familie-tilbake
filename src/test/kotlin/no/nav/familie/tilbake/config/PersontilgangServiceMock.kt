package no.nav.familie.tilbake.config

import no.nav.tilbakekreving.integrasjoner.CallContext
import no.nav.tilbakekreving.integrasjoner.persontilgang.Persontilgang
import no.nav.tilbakekreving.integrasjoner.persontilgang.PersontilgangService
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class PersontilgangServiceMock : PersontilgangService {
    override suspend fun sjekkPersontilgang(
        callContext: CallContext.Saksbehandler,
        personIdent: String,
    ): Persontilgang {
        return Persontilgang.Ok
    }
}
