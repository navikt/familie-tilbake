package no.nav.familie.tilbake.common

import no.nav.familie.tilbake.behandling.domain.Fagsystem
import no.nav.familie.tilbake.config.RolleConfig
import no.nav.familie.tilbake.sikkerhet.BehandlerRolle
import no.nav.familie.tilbake.sikkerhet.InnloggetBrukerTilgang
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder

object ContextService {

    private const val SYSTEM_FORKORTELSE = "VL"

    fun hentSaksbehandler(): String {
        return Result.runCatching { SpringTokenValidationContextHolder().tokenValidationContext }
                .fold(onSuccess = {
                    it.getClaims("azuread")?.get("preferred_username")?.toString() ?: SYSTEM_FORKORTELSE
                },
                      onFailure = { SYSTEM_FORKORTELSE })
    }

    private fun hentGrupper(): List<String> {
        return Result.runCatching { SpringTokenValidationContextHolder().tokenValidationContext }
                .fold(
                        onSuccess = {
                            @Suppress("UNCHECKED_CAST")
                            it.getClaims("azuread")?.get("groups") as List<String>? ?: emptyList()
                        },
                        onFailure = { emptyList() }
                )
    }

    fun hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(rolleConfig: RolleConfig): InnloggetBrukerTilgang {
        val saksbehandler = hentSaksbehandler()
        if (saksbehandler == SYSTEM_FORKORTELSE) return InnloggetBrukerTilgang(behandlerRolle = BehandlerRolle.SYSTEM)
        if (rolleConfig.ENVIRONMENT_NAME == "local") return InnloggetBrukerTilgang(behandlerRolle = BehandlerRolle.SYSTEM)

        val grupper = hentGrupper()

        when {
            grupper.contains(rolleConfig.BESLUTTER_ROLLE_BARNETRYGD) ->
                return InnloggetBrukerTilgang(behandlerRolle = BehandlerRolle.BESLUTTER, fagsystem = Fagsystem.BARNETRYGD)
            grupper.contains(rolleConfig.SAKSBEHANDLER_ROLLE_BARNETRYGD) ->
                return InnloggetBrukerTilgang(behandlerRolle = BehandlerRolle.SAKSBEHANDLER, fagsystem = Fagsystem.BARNETRYGD)
            grupper.contains(rolleConfig.VEILEDER_ROLLE_BARNETRYGD) ->
                return InnloggetBrukerTilgang(behandlerRolle = BehandlerRolle.VEILEDER, fagsystem = Fagsystem.BARNETRYGD)

            grupper.contains(rolleConfig.BESLUTTER_ROLLE_ENSLIG) ->
                return InnloggetBrukerTilgang(behandlerRolle = BehandlerRolle.BESLUTTER, fagsystem = Fagsystem.ENSLIG_FORELDER)
            grupper.contains(rolleConfig.SAKSBEHANDLER_ROLLE_ENSLIG) ->
                return InnloggetBrukerTilgang(behandlerRolle = BehandlerRolle.SAKSBEHANDLER,
                                              fagsystem = Fagsystem.ENSLIG_FORELDER)
            grupper.contains(rolleConfig.VEILEDER_ROLLE_ENSLIG) ->
                return InnloggetBrukerTilgang(behandlerRolle = BehandlerRolle.VEILEDER, fagsystem = Fagsystem.ENSLIG_FORELDER)

            grupper.contains(rolleConfig.BESLUTTER_ROLLE_KONTANTSTØTTE) ->
                return InnloggetBrukerTilgang(behandlerRolle = BehandlerRolle.BESLUTTER, fagsystem = Fagsystem.KONTANTSTØTTE)
            grupper.contains(rolleConfig.SAKSBEHANDLER_ROLLE_KONTANTSTØTTE) ->
                return InnloggetBrukerTilgang(behandlerRolle = BehandlerRolle.SAKSBEHANDLER, fagsystem = Fagsystem.KONTANTSTØTTE)
            grupper.contains(rolleConfig.VEILEDER_ROLLE_KONTANTSTØTTE) ->
                return InnloggetBrukerTilgang(behandlerRolle = BehandlerRolle.VEILEDER, fagsystem = Fagsystem.KONTANTSTØTTE)
            else -> return InnloggetBrukerTilgang(behandlerRolle = BehandlerRolle.UKJENT)
        }
    }

}
