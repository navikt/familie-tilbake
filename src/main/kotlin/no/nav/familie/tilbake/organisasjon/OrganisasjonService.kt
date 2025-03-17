package no.nav.familie.tilbake.organisasjon

import no.nav.familie.tilbake.dokumentbestilling.felles.header.Institusjon
import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.kontrakter.organisasjon.Organisasjon
import no.nav.tilbakekreving.api.v1.dto.InstitusjonDto
import org.springframework.stereotype.Service

@Service
class OrganisasjonService(
    private val integrasjonerClient: IntegrasjonerClient,
) {
    fun mapTilInstitusjonDto(orgnummer: String): InstitusjonDto {
        val organisasjon = hentOrganisasjon(orgnummer)
        return InstitusjonDto(organisasjonsnummer = orgnummer, navn = organisasjon.navn)
    }

    fun mapTilInstitusjonForBrevgenerering(orgnummer: String): Institusjon {
        val organisasjon = hentOrganisasjon(orgnummer)
        return Institusjon(organisasjonsnummer = orgnummer, navn = organisasjon.navn)
    }

    private fun hentOrganisasjon(orgnummer: String): Organisasjon {
        val organisasjon = integrasjonerClient.hentOrganisasjon(orgnummer)
        return organisasjon
    }
}
