package no.nav.tilbakekreving.feil

import no.nav.tilbakekreving.UtenforScope

sealed class ModellFeil(
    val melding: String,
    val feilLogg: Sporing,
) : Exception(melding) {
    class UgyldigOperasjonException(
        melding: String,
        feilLogg: Sporing,
    ) : ModellFeil(melding, feilLogg)

    class UtenforScopeException(
        utenforScope: UtenforScope,
        feilLogg: Sporing,
    ) : ModellFeil(utenforScope.feilmelding, feilLogg)
}

data class Sporing(
    val fagsakId: String,
    val behandlingId: String,
)
