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
        val innloggetBrukertilgang = InnloggetBrukertilgang()
        if (saksbehandler == SYSTEM_FORKORTELSE || environment.activeProfiles.any { "local" == it }) {
            innloggetBrukertilgang.leggTilTilgangerMedRolle(fagsystem = Fagsystem.SYSTEM_TILGANG,
                                                            behandlerrolle = Behandlerrolle.SYSTEM)
        }
        val grupper = hentGrupper()

        if (grupper.contains(rolleConfig.beslutterRolleBarnetrygd)) {
            innloggetBrukertilgang.leggTilTilgangerMedRolle(fagsystem = Fagsystem.BARNETRYGD,
                                                            behandlerrolle = Behandlerrolle.BESLUTTER)
        }
        if (grupper.contains(rolleConfig.saksbehandlerRolleBarnetrygd)) {
            innloggetBrukertilgang.leggTilTilgangerMedRolle(fagsystem = Fagsystem.BARNETRYGD,
                                                            behandlerrolle = Behandlerrolle.SAKSBEHANDLER)
        }
        if (grupper.contains(rolleConfig.veilederRolleBarnetrygd)) {
            innloggetBrukertilgang.leggTilTilgangerMedRolle(fagsystem = Fagsystem.BARNETRYGD,
                                                            behandlerrolle = Behandlerrolle.VEILEDER)
        }
        if (grupper.contains(rolleConfig.beslutterRolleEnslig)) {
            innloggetBrukertilgang.leggTilTilgangerMedRolle(fagsystem = Fagsystem.ENSLIG_FORELDER,
                                                            behandlerrolle = Behandlerrolle.BESLUTTER)
        }
        if (grupper.contains(rolleConfig.saksbehandlerRolleEnslig)) {
            innloggetBrukertilgang.leggTilTilgangerMedRolle(fagsystem = Fagsystem.ENSLIG_FORELDER,
                                                            behandlerrolle = Behandlerrolle.SAKSBEHANDLER)
        }
        if (grupper.contains(rolleConfig.veilederRolleEnslig)) {
            innloggetBrukertilgang.leggTilTilgangerMedRolle(fagsystem = Fagsystem.ENSLIG_FORELDER,
                                                            behandlerrolle = Behandlerrolle.VEILEDER)
        }
        if (grupper.contains(rolleConfig.beslutterRolleKontantStøtte)) {
            innloggetBrukertilgang.leggTilTilgangerMedRolle(fagsystem = Fagsystem.KONTANTSTØTTE,
                                                            behandlerrolle = Behandlerrolle.BESLUTTER)
        }
        if (grupper.contains(rolleConfig.saksbehandlerRolleKontantStøtte)) {
            innloggetBrukertilgang.leggTilTilgangerMedRolle(fagsystem = Fagsystem.KONTANTSTØTTE,
                                                            behandlerrolle = Behandlerrolle.SAKSBEHANDLER)
        }
        if (grupper.contains(rolleConfig.saksbehandlerRolleKontantStøtte)) {
            innloggetBrukertilgang.leggTilTilgangerMedRolle(fagsystem = Fagsystem.KONTANTSTØTTE,
                                                            behandlerrolle = Behandlerrolle.VEILEDER)
        }
        if (innloggetBrukertilgang.tilganger.isEmpty()) {
            throw Feil(message = "Bruker har ukjente grupper=$grupper, har ikke tilgang til $handling",
                       frontendFeilmelding = "Bruker har ukjente grupper=$grupper, har ikke tilgang til $handling",
                       httpStatus = HttpStatus.FORBIDDEN)
        }
        return innloggetBrukertilgang
    }

}
