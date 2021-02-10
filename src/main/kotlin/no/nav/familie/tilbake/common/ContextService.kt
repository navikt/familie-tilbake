package no.nav.familie.tilbake.common

import no.nav.familie.tilbake.behandling.domain.Fagsystem
import no.nav.familie.tilbake.config.RolleConfig
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.InnloggetBrukertilgang
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.core.env.Environment

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

    fun hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(rolleConfig: RolleConfig,
                                                               environment: Environment): InnloggetBrukertilgang {
        val saksbehandler = hentSaksbehandler()
        if (saksbehandler == SYSTEM_FORKORTELSE) return InnloggetBrukertilgang(behandlerrolle = Behandlerrolle.SYSTEM)
        if (environment.activeProfiles.any { "local" == it }) {
            return InnloggetBrukertilgang(behandlerrolle = Behandlerrolle.SYSTEM)
        }

        val grupper = hentGrupper()

        return when {
            grupper.contains(rolleConfig.beslutterRolleBarnetrygd) ->
                InnloggetBrukertilgang(behandlerrolle = Behandlerrolle.BESLUTTER, fagsystem = Fagsystem.BARNETRYGD)
            grupper.contains(rolleConfig.saksbehandlerRolleBarnetrygd) ->
                InnloggetBrukertilgang(behandlerrolle = Behandlerrolle.SAKSBEHANDLER, fagsystem = Fagsystem.BARNETRYGD)
            grupper.contains(rolleConfig.veilederRolleBarnetrygd) ->
                InnloggetBrukertilgang(behandlerrolle = Behandlerrolle.VEILEDER, fagsystem = Fagsystem.BARNETRYGD)

            grupper.contains(rolleConfig.beslutterRolleEnslig) ->
                InnloggetBrukertilgang(behandlerrolle = Behandlerrolle.BESLUTTER, fagsystem = Fagsystem.ENSLIG_FORELDER)
            grupper.contains(rolleConfig.saksbehandlerRolleEnslig) ->
                InnloggetBrukertilgang(behandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                                       fagsystem = Fagsystem.ENSLIG_FORELDER)
            grupper.contains(rolleConfig.veilederRolleEnslig) ->
                InnloggetBrukertilgang(behandlerrolle = Behandlerrolle.VEILEDER, fagsystem = Fagsystem.ENSLIG_FORELDER)

            grupper.contains(rolleConfig.beslutterRolleKontantStøtte) ->
                InnloggetBrukertilgang(behandlerrolle = Behandlerrolle.BESLUTTER, fagsystem = Fagsystem.KONTANTSTØTTE)
            grupper.contains(rolleConfig.saksbehandlerRolleKontantStøtte) ->
                InnloggetBrukertilgang(behandlerrolle = Behandlerrolle.SAKSBEHANDLER, fagsystem = Fagsystem.KONTANTSTØTTE)
            grupper.contains(rolleConfig.veilederRolleKontantStøtte) ->
                InnloggetBrukertilgang(behandlerrolle = Behandlerrolle.VEILEDER, fagsystem = Fagsystem.KONTANTSTØTTE)
            else -> InnloggetBrukertilgang(behandlerrolle = Behandlerrolle.UKJENT)
        }
    }

}
