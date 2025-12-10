package no.nav.tilbakekreving.integrasjoner.arbeidsforhold

import no.nav.familie.tilbake.kontrakter.organisasjon.Organisasjon
import org.springframework.context.annotation.Profile

@Profile("e2e", "local", "integrasjonstest")
class EregClientStub() : EregClient {
    override fun hentOrganisasjon(orgnr: String): Organisasjon {
        return Organisasjon(
            organisasjonsnummer = orgnr,
            navn = "sammensattnavn",
        )
    }
}
