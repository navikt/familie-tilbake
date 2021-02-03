package no.nav.familie.tilbake.common

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.config.RolleConfig
import org.springframework.stereotype.Service

@Service
class TilgangService(private val rolleConfig: RolleConfig) {

    /**
     * Sjekk om saksbehandler har tilgang til å gjøre en gitt handling.
     *
     * @minimumBehandlerRolle den laveste rolle som kreves for den angitte handlingen
     * @handling kort beskrivelse for handlingen. Eksempel: 'endre vilkår', 'oppprette behandling'.
     * Handlingen kommer til saksbehandler så det er viktig at denne gir mening.
     */
    fun verifiserHarTilgangTilHandling(minimumBehandlerRolle: BehandlerRolle, handling: String) {
        val høyesteRolletilgang = ContextService.hentHøyesteRolletilgangForInnloggetBruker(rolleConfig)

        if (minimumBehandlerRolle.nivå > høyesteRolletilgang.nivå) {
            throw Feil(
                    message = "${ContextService.hentSaksbehandler()} med rolle $høyesteRolletilgang " +
                              "har ikke tilgang til å $handling. Krever $minimumBehandlerRolle.",
                    frontendFeilmelding = "Du har ikke tilgang til å $handling."
            )
        }
    }
}
