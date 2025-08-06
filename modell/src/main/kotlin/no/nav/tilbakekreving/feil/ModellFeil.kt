package no.nav.tilbakekreving.feil

import no.nav.tilbakekreving.UtenforScope

sealed class ModellFeil(
    val melding: String,
    val sporing: Sporing,
) : Exception(melding) {
    class UgyldigOperasjonException(
        melding: String,
        sporing: Sporing,
    ) : ModellFeil(melding, sporing)

    class UtenforScopeException(
        val utenforScope: UtenforScope,
        sporing: Sporing,
    ) : ModellFeil(utenforScope.feilmelding, sporing)
}

data class Sporing(
    val fagsakId: String,
    val behandlingId: String,
)
