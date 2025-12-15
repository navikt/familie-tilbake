package no.tilbakekreving.integrasjoner.azure.kontrakter

import java.util.UUID

data class AzureAdBrukere(
    val value: List<AzureAdBruker>,
)

data class AzureAdBruker(
    val id: UUID,
    val onPremisesSamAccountName: String,
    val userPrincipalName: String,
    val givenName: String,
    val surname: String,
    val streetAddress: String,
    val city: String,
)
