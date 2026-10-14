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

    class BehandlingIkkeEndretException(
        melding: String,
        sporing: Sporing,
    ) : ModellFeil(
            "Behandling ble ikke endret",
            melding,
            sporing,
        )

    class TjenesteUtilgjengeligException(
        sporing: Sporing,
    ) : ModellFeil(
            "Fryseperiode 9. oktober kl. 16:00–19. oktober kl. 08:00",
            "Skatteetaten avvikler PAK og migrerer til Innfri. I denne perioden er det ikke mulig å sende vedtak til beslutter i Tilbakeløsningen.",
            sporing,
        )
}

data class Sporing(
    val fagsakId: String,
    val behandlingId: String?,
)
