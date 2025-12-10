package no.nav.tilbakekreving.integrasjoner.arbeidsforhold

import no.nav.familie.tilbake.kontrakter.organisasjon.Organisasjon

interface EregClient {
    fun hentOrganisasjon(orgnr: String): Organisasjon

    fun validerOrganisasjon(orgnr: String): Boolean
}
