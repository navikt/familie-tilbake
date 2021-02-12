package no.nav.familie.tilbake.common

import no.nav.familie.tilbake.behandling.domain.Fagsystem
import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.config.RolleConfig
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.InnloggetBrukertilgang
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus

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
                                                               handling: String,
                                                               environment: Environment): InnloggetBrukertilgang {
        val saksbehandler = hentSaksbehandler()

        val rollerMedTilgang =
                hentGrupper().map {
                    rolleConfig.rolleMap[it]
                }.filterNotNull()
                        .toMap()

        if (saksbehandler == SYSTEM_FORKORTELSE || environment.activeProfiles.any { "local" == it }) {
            return InnloggetBrukertilgang(rollerMedTilgang.plus(Fagsystem.SYSTEM_TILGANG to Behandlerrolle.SYSTEM))
        }

        if (rollerMedTilgang.isEmpty()) {
            throw Feil(message = "Bruker har mangler tilgang til $handling",
                       frontendFeilmelding = "Bruker har mangler tilgang til $handling",
                       httpStatus = HttpStatus.FORBIDDEN)
        }

        return InnloggetBrukertilgang(rollerMedTilgang)
    }
}
