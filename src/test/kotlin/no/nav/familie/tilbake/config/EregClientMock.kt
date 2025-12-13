package no.nav.familie.tilbake.config

import no.tilbakekreving.integrasjoner.arbeidsforhold.EregClient
import no.tilbakekreving.integrasjoner.arbeidsforhold.kontrakter.HentOrganisasjonResponse
import no.tilbakekreving.integrasjoner.arbeidsforhold.kontrakter.Navn
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Service
@Primary
class EregClientMock : EregClient {
    override fun hentOrganisasjon(orgnr: String): HentOrganisasjonResponse {
        return HentOrganisasjonResponse(Navn("Testinstitusjon"), null)
    }
}
