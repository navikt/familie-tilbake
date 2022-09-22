package no.nav.familie.tilbake.organisasjon

import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import org.springframework.stereotype.Service

@Service
class OrganisasjonService(private val integrasjonerClient: IntegrasjonerClient) {

    fun hentOrganisasjonNavn(orgnummer: String): String {
        val organisasjon = integrasjonerClient.hentOrganisasjon(orgnummer)
        return organisasjon.navn
    }
}
