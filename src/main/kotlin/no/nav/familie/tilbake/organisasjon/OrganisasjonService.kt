package no.nav.familie.tilbake.organisasjon

import no.nav.familie.tilbake.integration.familie.IntegrasjonerClient
import no.nav.familie.tilbake.kontrakter.organisasjon.Organisasjon
import no.nav.tilbakekreving.Toggle
import no.nav.tilbakekreving.api.v1.dto.InstitusjonDto
import no.nav.tilbakekreving.arbeidsforhold.ArbeidsforholdService
import no.nav.tilbakekreving.config.FeatureService
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.header.Institusjon
import org.springframework.stereotype.Service

@Service
class OrganisasjonService(
    private val integrasjonerClient: IntegrasjonerClient,
    private val featureService: FeatureService,
    private val arbeidsforholdService: ArbeidsforholdService,
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
        if (featureService.modellFeatures[Toggle.EregServices]) {
            return arbeidsforholdService.hentOrganisasjon(orgnummer)
        }
        val organisasjon = integrasjonerClient.hentOrganisasjon(orgnummer)
        return organisasjon
    }
}
