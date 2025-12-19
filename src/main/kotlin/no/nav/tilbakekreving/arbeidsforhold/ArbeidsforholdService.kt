package no.nav.tilbakekreving.arbeidsforhold

import no.nav.familie.tilbake.kontrakter.organisasjon.Organisasjon
import no.tilbakekreving.integrasjoner.arbeidsforhold.EregClient
import no.tilbakekreving.integrasjoner.feil.NotFoundException
import org.springframework.stereotype.Service

@Service
class ArbeidsforholdService(
    private val eregClient: EregClient,
) {
    fun hentOrganisasjon(orgnr: String): Organisasjon {
        val organisasjonResponse = eregClient.hentOrganisasjon(orgnr)
        return Organisasjon(
            organisasjonsnummer = orgnr,
            navn = organisasjonResponse.navn.sammensattnavn,
        )
    }

    fun validerOrganisasjon(orgnr: String): Boolean =
        try {
            hentOrganisasjon(orgnr)
            true
        } catch (e: NotFoundException) {
            false
        }
}
