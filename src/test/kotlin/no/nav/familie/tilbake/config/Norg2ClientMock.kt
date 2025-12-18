package no.nav.familie.tilbake.config

import no.tilbakekreving.integrasjoner.norg2.Norg2Client
import no.tilbakekreving.integrasjoner.norg2.kontrakter.NavKontorEnhet
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class Norg2ClientMock : Norg2Client {
    override fun hentNavkontor(enhetId: String): NavKontorEnhet {
        return NavKontorEnhet(
            enhetId = 4806,
            navn = "Mock Nav Drammen",
            enhetNr = "mock",
            status = "mock",
        )
    }
}
