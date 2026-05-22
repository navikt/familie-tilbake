package no.nav.tilbakekreving.feil

import no.nav.tilbakekreving.UtenforScope

sealed class ModellFeil(
    val tittel: String,
    val melding: String,
    val sporing: Sporing,
) : Exception(melding) {
    class UgyldigOperasjonException(
        melding: String,
        sporing: Sporing,
    ) : ModellFeil("Kan ikke utføre handling", melding, sporing)

    class UtenforScopeException(
        val utenforScope: UtenforScope,
        sporing: Sporing,
    ) : ModellFeil(utenforScope.tittel, utenforScope.feilmelding, sporing)

    class IngenTilgangException(
        melding: String,
        sporing: Sporing,
    ) : ModellFeil("Du mangler nødvendig tilgang", melding, sporing)
}

data class Sporing(
    val fagsakId: String,
    val behandlingId: String?,
)
