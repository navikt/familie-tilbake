package no.nav.tilbakekreving.arbeidsforhold

import no.tilbakekreving.integrasjoner.arbeidsforhold.EregClient
import no.tilbakekreving.integrasjoner.arbeidsforhold.kontrakter.HentOrganisasjonResponse
import no.tilbakekreving.integrasjoner.arbeidsforhold.kontrakter.Navn

class EregClientStub : EregClient {
    override fun hentOrganisasjon(orgnr: String): HentOrganisasjonResponse {
        return HentOrganisasjonResponse(
            navn = Navn("Testinstitusjon"),
            null,
        )
    }
}
