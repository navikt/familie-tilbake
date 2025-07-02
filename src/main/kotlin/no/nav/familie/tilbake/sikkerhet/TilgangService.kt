package no.nav.familie.tilbake.sikkerhet

import no.nav.familie.tilbake.common.ContextService
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.config.ApplicationProperties
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import org.springframework.stereotype.Service

@Service
class TilgangService(
    private val applicationProperties: ApplicationProperties,
) {
    fun tilgangTilÅOppretteRevurdering(fagsystem: FagsystemDTO): Boolean = finnBehandlerrolle(fagsystem) !in listOf(Behandlerrolle.VEILEDER, Behandlerrolle.FORVALTER)

    fun finnBehandlerrolle(fagsystem: FagsystemDTO): Behandlerrolle? {
        val inloggetBrukerstilgang = ContextService
            .hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(applicationProperties.tilgangsstyring, "henter behandling", SecureLog.Context.tom())

        val tilganger = inloggetBrukerstilgang.tilganger
        var behandlerrolle: Behandlerrolle? = Behandlerrolle.VEILEDER
        if (tilganger.containsKey(Tilgangskontrollsfagsystem.SYSTEM_TILGANG)) {
            behandlerrolle = Behandlerrolle.SYSTEM
        }
        if (tilganger.containsKey(Tilgangskontrollsfagsystem.FORVALTER_TILGANG)) {
            behandlerrolle = Behandlerrolle.FORVALTER
        }
        if (tilganger.containsKey(Tilgangskontrollsfagsystem.fraFagsystem(fagsystem))) {
            behandlerrolle = tilganger[Tilgangskontrollsfagsystem.fraFagsystem(fagsystem)]
        }
        return behandlerrolle
    }

    fun harInnloggetBrukerForvalterRolle(): Boolean {
        val innloggetBrukerstilgang = ContextService
            .hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(applicationProperties.tilgangsstyring, "henter behandling", SecureLog.Context.tom())

        return innloggetBrukerstilgang.tilganger.containsKey(Tilgangskontrollsfagsystem.FORVALTER_TILGANG)
    }
}
