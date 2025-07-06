package no.nav.familie.tilbake.config

import no.tilbakekreving.integrasjoner.CallContext
import no.tilbakekreving.integrasjoner.persontilgang.Persontilgang
import no.tilbakekreving.integrasjoner.persontilgang.PersontilgangService
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
