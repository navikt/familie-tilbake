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
        val brukerTilganger = mutableMapOf<Fagsystem, Behandlerrolle>()
        if (saksbehandler == SYSTEM_FORKORTELSE || environment.activeProfiles.any { "local" == it }) {
            brukerTilganger[Fagsystem.SYSTEM_TILGANG] = Behandlerrolle.SYSTEM
        }
        val grupper = hentGrupper()

        if (grupper.contains(rolleConfig.beslutterRolleBarnetrygd)) {
            brukerTilganger.putAll(hentTilgangMedRolle(fagsystem = Fagsystem.BARNETRYGD,
                                                       behandlerrolle = Behandlerrolle.BESLUTTER,
                                                       brukerTilganger = brukerTilganger))
        }
        if (grupper.contains(rolleConfig.saksbehandlerRolleBarnetrygd)) {
            brukerTilganger.putAll(hentTilgangMedRolle(fagsystem = Fagsystem.BARNETRYGD,
                                                       behandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                                                       brukerTilganger = brukerTilganger))
        }
        if (grupper.contains(rolleConfig.veilederRolleBarnetrygd)) {
            brukerTilganger.putAll(hentTilgangMedRolle(fagsystem = Fagsystem.BARNETRYGD,
                                                       behandlerrolle = Behandlerrolle.VEILEDER,
                                                       brukerTilganger = brukerTilganger))
        }
        if (grupper.contains(rolleConfig.beslutterRolleEnslig)) {
            brukerTilganger.putAll(hentTilgangMedRolle(fagsystem = Fagsystem.ENSLIG_FORELDER,
                                                       behandlerrolle = Behandlerrolle.BESLUTTER,
                                                       brukerTilganger = brukerTilganger))
        }
        if (grupper.contains(rolleConfig.saksbehandlerRolleEnslig)) {
            brukerTilganger.putAll(hentTilgangMedRolle(fagsystem = Fagsystem.ENSLIG_FORELDER,
                                                       behandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                                                       brukerTilganger = brukerTilganger))
        }
        if (grupper.contains(rolleConfig.veilederRolleEnslig)) {
            brukerTilganger.putAll(hentTilgangMedRolle(fagsystem = Fagsystem.ENSLIG_FORELDER,
                                                       behandlerrolle = Behandlerrolle.VEILEDER,
                                                       brukerTilganger = brukerTilganger))
        }
        if (grupper.contains(rolleConfig.beslutterRolleKontantStøtte)) {
            brukerTilganger.putAll(hentTilgangMedRolle(fagsystem = Fagsystem.KONTANTSTØTTE,
                                                       behandlerrolle = Behandlerrolle.BESLUTTER,
                                                       brukerTilganger = brukerTilganger))
        }
        if (grupper.contains(rolleConfig.saksbehandlerRolleKontantStøtte)) {
            brukerTilganger.putAll(hentTilgangMedRolle(fagsystem = Fagsystem.KONTANTSTØTTE,
                                                       behandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                                                       brukerTilganger = brukerTilganger))
        }
        if (grupper.contains(rolleConfig.veilederRolleKontantStøtte)) {
            brukerTilganger.putAll(hentTilgangMedRolle(fagsystem = Fagsystem.KONTANTSTØTTE,
                                                       behandlerrolle = Behandlerrolle.VEILEDER,
                                                       brukerTilganger = brukerTilganger))
        }
        if (brukerTilganger.isEmpty()) {
            throw Feil(message = "Bruker har mangler tilgang til $handling",
                       frontendFeilmelding = "Bruker har mangler tilgang til $handling",
                       httpStatus = HttpStatus.FORBIDDEN)
        }

        return InnloggetBrukertilgang(brukerTilganger.toMap())
    }

    private fun hentTilgangMedRolle(fagsystem: Fagsystem,
                                    behandlerrolle: Behandlerrolle,
                                    brukerTilganger: Map<Fagsystem, Behandlerrolle>): Map<Fagsystem, Behandlerrolle> {
        if (!harBrukerAlleredeHøyereTilgangPåSammeFagssystem(fagsystem, behandlerrolle, brukerTilganger)) {
            return mapOf(fagsystem to behandlerrolle)
        }
        return emptyMap()
    }

    private fun harBrukerAlleredeHøyereTilgangPåSammeFagssystem(fagsystem: Fagsystem,
                                                                behandlerrolle: Behandlerrolle,
                                                                brukerTilganger: Map<Fagsystem, Behandlerrolle>): Boolean {
        if (brukerTilganger.containsKey(fagsystem)) {
            return brukerTilganger[fagsystem]!!.nivå > behandlerrolle.nivå
        }
        return false
    }
}
