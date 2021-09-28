package no.nav.familie.tilbake.sikkerhet

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.config.RolleConfig
import org.springframework.stereotype.Service

@Service
class TilgangService(private val rolleConfig: RolleConfig) {

    fun tilgangTilÅOppretteRevurdering(fagsystem: Fagsystem): Boolean {
        return finnBehandlerrolle(fagsystem) != Behandlerrolle.VEILEDER
    }

    fun finnBehandlerrolle(fagsystem: Fagsystem): Behandlerrolle? {
        val inloggetBrukerstilgang = ContextService
                .hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(rolleConfig, "henter behandling")

        val tilganger = inloggetBrukerstilgang.tilganger

        return if (tilganger.containsKey(Tilgangskontrollsfagsystem.SYSTEM_TILGANG)) Behandlerrolle.SYSTEM
        else tilganger[Tilgangskontrollsfagsystem.fraFagsystem(fagsystem)]
    }


}