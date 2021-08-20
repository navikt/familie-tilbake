package no.nav.familie.tilbake.common

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.config.RolleConfig
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.InnloggetBrukertilgang
import no.nav.familie.tilbake.sikkerhet.Tilgangskontrollsfagsystem
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import org.springframework.http.HttpStatus
import org.springframework.jca.context.SpringContextResourceAdapter

object ContextService {


    fun hentSaksbehandler(): String {
        return Result.runCatching { SpringTokenValidationContextHolder().tokenValidationContext }
                .fold(onSuccess = {
                    it.getClaims("azuread")?.get("NAVident")?.toString() ?: Constants.BRUKER_ID_VEDTAKSLØSNINGEN
                },
                      onFailure = { Constants.BRUKER_ID_VEDTAKSLØSNINGEN })
    }

    private fun hentGrupper(): List<String> {
        return Result.runCatching { SpringTokenValidationContextHolder().tokenValidationContext }
                .fold(onSuccess = {
                    @Suppress("UNCHECKED_CAST")
                    it.getClaims("azuread")?.get("groups") as List<String>? ?: emptyList()
                },
                      onFailure = { emptyList() })
    }

    fun hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(rolleConfig: RolleConfig,
                                                               handling: String): InnloggetBrukertilgang {
        val saksbehandler = hentSaksbehandler()
        val brukerTilganger = mutableMapOf<Tilgangskontrollsfagsystem, Behandlerrolle>()
        if (saksbehandler == Constants.BRUKER_ID_VEDTAKSLØSNINGEN) {
            brukerTilganger[Tilgangskontrollsfagsystem.SYSTEM_TILGANG] = Behandlerrolle.SYSTEM
        }
        val grupper = hentGrupper()

        if (grupper.contains(rolleConfig.beslutterRolleBarnetrygd)) {
            brukerTilganger.putAll(hentTilgangMedRolle(fagsystem = Tilgangskontrollsfagsystem.BARNETRYGD,
                                                       behandlerrolle = Behandlerrolle.BESLUTTER,
                                                       brukerTilganger = brukerTilganger))
        }
        if (grupper.contains(rolleConfig.saksbehandlerRolleBarnetrygd)) {
            brukerTilganger.putAll(hentTilgangMedRolle(fagsystem = Tilgangskontrollsfagsystem.BARNETRYGD,
                                                       behandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                                                       brukerTilganger = brukerTilganger))
        }
        if (grupper.contains(rolleConfig.veilederRolleBarnetrygd)) {
            brukerTilganger.putAll(hentTilgangMedRolle(fagsystem = Tilgangskontrollsfagsystem.BARNETRYGD,
                                                       behandlerrolle = Behandlerrolle.VEILEDER,
                                                       brukerTilganger = brukerTilganger))
        }
        if (grupper.contains(rolleConfig.beslutterRolleEnslig)) {
            brukerTilganger.putAll(hentTilgangMedRolle(fagsystem = Tilgangskontrollsfagsystem.ENSLIG_FORELDER,
                                                       behandlerrolle = Behandlerrolle.BESLUTTER,
                                                       brukerTilganger = brukerTilganger))
        }
        if (grupper.contains(rolleConfig.saksbehandlerRolleEnslig)) {
            brukerTilganger.putAll(hentTilgangMedRolle(fagsystem = Tilgangskontrollsfagsystem.ENSLIG_FORELDER,
                                                       behandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                                                       brukerTilganger = brukerTilganger))
        }
        if (grupper.contains(rolleConfig.veilederRolleEnslig)) {
            brukerTilganger.putAll(hentTilgangMedRolle(fagsystem = Tilgangskontrollsfagsystem.ENSLIG_FORELDER,
                                                       behandlerrolle = Behandlerrolle.VEILEDER,
                                                       brukerTilganger = brukerTilganger))
        }
        if (grupper.contains(rolleConfig.beslutterRolleKontantStøtte)) {
            brukerTilganger.putAll(hentTilgangMedRolle(fagsystem = Tilgangskontrollsfagsystem.KONTANTSTØTTE,
                                                       behandlerrolle = Behandlerrolle.BESLUTTER,
                                                       brukerTilganger = brukerTilganger))
        }
        if (grupper.contains(rolleConfig.saksbehandlerRolleKontantStøtte)) {
            brukerTilganger.putAll(hentTilgangMedRolle(fagsystem = Tilgangskontrollsfagsystem.KONTANTSTØTTE,
                                                       behandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                                                       brukerTilganger = brukerTilganger))
        }
        if (grupper.contains(rolleConfig.veilederRolleKontantStøtte)) {
            brukerTilganger.putAll(hentTilgangMedRolle(fagsystem = Tilgangskontrollsfagsystem.KONTANTSTØTTE,
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

    private fun hentTilgangMedRolle(fagsystem: Tilgangskontrollsfagsystem,
                                    behandlerrolle: Behandlerrolle,
                                    brukerTilganger: Map<Tilgangskontrollsfagsystem, Behandlerrolle>)
            : Map<Tilgangskontrollsfagsystem, Behandlerrolle> {
        if (!harBrukerAlleredeHøyereTilgangPåSammeFagssystem(fagsystem, behandlerrolle, brukerTilganger)) {
            return mapOf(fagsystem to behandlerrolle)
        }
        return emptyMap()
    }

    private fun harBrukerAlleredeHøyereTilgangPåSammeFagssystem(fagsystem: Tilgangskontrollsfagsystem,
                                                                behandlerrolle: Behandlerrolle,
                                                                brukerTilganger: Map<Tilgangskontrollsfagsystem, Behandlerrolle>)
            : Boolean {
        if (brukerTilganger.containsKey(fagsystem)) {
            return brukerTilganger[fagsystem]!!.nivå > behandlerrolle.nivå
        }
        return false
    }
}
