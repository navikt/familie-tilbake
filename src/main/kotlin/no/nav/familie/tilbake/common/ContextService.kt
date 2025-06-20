package no.nav.familie.tilbake.common

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.exceptionhandler.ForbiddenError
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.config.RolleConfig
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.InnloggetBrukertilgang
import no.nav.familie.tilbake.sikkerhet.Tilgangskontrollsfagsystem
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.tilbakekreving.saksbehandler.Behandler

object ContextService {
    private const val SYSTEM_NAVN = "System"

    fun hentSaksbehandler(logContext: SecureLog.Context): String = hentPåloggetSaksbehandler(Constants.BRUKER_ID_VEDTAKSLØSNINGEN, logContext)

    fun hentBehandler(logContext: SecureLog.Context): Behandler = Behandler.Saksbehandler(hentPåloggetSaksbehandler(defaultverdi = null, logContext = logContext))

    fun hentPåloggetSaksbehandler(
        defaultverdi: String?,
        logContext: SecureLog.Context,
    ): String {
        return Result
            .runCatching { SpringTokenValidationContextHolder().getTokenValidationContext() }
            .fold(
                onSuccess = {
                    return it.getAzureadClaimsOrNull()?.get("NAVident")?.toString()
                        ?: defaultverdi
                        ?: throw Feil(
                            message = "Ingen defaultverdi for bruker ved maskinelt oppslag",
                            logContext = logContext,
                        )
                },
                onFailure = {
                    defaultverdi ?: throw Feil(
                        message = "Ingen defaultverdi for bruker ved maskinelt oppslag",
                        logContext = logContext,
                    )
                },
            )
    }

    // TODO Fjern hack for midlertidig simulere oppførsel fra tidligere versjon av no.nav.security.token.support.core
    @Deprecated("Ikke bruk! kommer til å byttes ut med versjon som kaster exception ved manglende issuer claims i neste versjon av tilbake")
    private fun TokenValidationContext.getAzureadClaimsOrNull(): JwtTokenClaims? =
        try {
            this.getClaims("azuread")
        } catch (e: IllegalArgumentException) {
            null
        }

    fun hentSaksbehandlerNavn(strict: Boolean = false): String =
        Result
            .runCatching { SpringTokenValidationContextHolder().getTokenValidationContext() }
            .fold(
                onSuccess = {
                    it.getAzureadClaimsOrNull()?.get("name")?.toString()
                        ?: if (strict) error("Finner ikke navn i azuread token") else SYSTEM_NAVN
                },
                onFailure = { if (strict) error("Finner ikke navn på innlogget bruker") else SYSTEM_NAVN },
            )

    private fun hentGrupper(): List<String> =
        Result
            .runCatching { SpringTokenValidationContextHolder().getTokenValidationContext() }
            .fold(
                onSuccess = {
                    @Suppress("UNCHECKED_CAST")
                    it.getClaims("azuread")?.get("groups") as List<String>? ?: emptyList()
                },
                onFailure = { emptyList() },
            )

    fun hentHøyesteRolletilgangOgYtelsestypeForInnloggetBruker(
        rolleConfig: RolleConfig,
        handling: String,
        logContext: SecureLog.Context,
    ): InnloggetBrukertilgang {
        val saksbehandler = hentSaksbehandler(logContext)
        val brukerTilganger = mutableMapOf<Tilgangskontrollsfagsystem, Behandlerrolle>()
        if (saksbehandler == Constants.BRUKER_ID_VEDTAKSLØSNINGEN) {
            brukerTilganger[Tilgangskontrollsfagsystem.SYSTEM_TILGANG] = Behandlerrolle.SYSTEM
        }
        val grupper = hentGrupper()

        if (grupper.contains(rolleConfig.beslutterRolleBarnetrygd)) {
            brukerTilganger.putAll(
                hentTilgangMedRolle(
                    fagsystem = Tilgangskontrollsfagsystem.BARNETRYGD,
                    behandlerrolle = Behandlerrolle.BESLUTTER,
                    brukerTilganger = brukerTilganger,
                ),
            )
        }
        if (grupper.contains(rolleConfig.saksbehandlerRolleBarnetrygd)) {
            brukerTilganger.putAll(
                hentTilgangMedRolle(
                    fagsystem = Tilgangskontrollsfagsystem.BARNETRYGD,
                    behandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                    brukerTilganger = brukerTilganger,
                ),
            )
        }
        if (grupper.contains(rolleConfig.veilederRolleBarnetrygd)) {
            brukerTilganger.putAll(
                hentTilgangMedRolle(
                    fagsystem = Tilgangskontrollsfagsystem.BARNETRYGD,
                    behandlerrolle = Behandlerrolle.VEILEDER,
                    brukerTilganger = brukerTilganger,
                ),
            )
        }
        if (grupper.contains(rolleConfig.beslutterRolleEnslig)) {
            brukerTilganger.putAll(
                hentTilgangMedRolle(
                    fagsystem = Tilgangskontrollsfagsystem.ENSLIG_FORELDER,
                    behandlerrolle = Behandlerrolle.BESLUTTER,
                    brukerTilganger = brukerTilganger,
                ),
            )
        }
        if (grupper.contains(rolleConfig.saksbehandlerRolleEnslig)) {
            brukerTilganger.putAll(
                hentTilgangMedRolle(
                    fagsystem = Tilgangskontrollsfagsystem.ENSLIG_FORELDER,
                    behandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                    brukerTilganger = brukerTilganger,
                ),
            )
        }
        if (grupper.contains(rolleConfig.veilederRolleEnslig)) {
            brukerTilganger.putAll(
                hentTilgangMedRolle(
                    fagsystem = Tilgangskontrollsfagsystem.ENSLIG_FORELDER,
                    behandlerrolle = Behandlerrolle.VEILEDER,
                    brukerTilganger = brukerTilganger,
                ),
            )
        }
        if (grupper.contains(rolleConfig.beslutterRolleKontantStøtte)) {
            brukerTilganger.putAll(
                hentTilgangMedRolle(
                    fagsystem = Tilgangskontrollsfagsystem.KONTANTSTØTTE,
                    behandlerrolle = Behandlerrolle.BESLUTTER,
                    brukerTilganger = brukerTilganger,
                ),
            )
        }
        if (grupper.contains(rolleConfig.saksbehandlerRolleKontantStøtte)) {
            brukerTilganger.putAll(
                hentTilgangMedRolle(
                    fagsystem = Tilgangskontrollsfagsystem.KONTANTSTØTTE,
                    behandlerrolle = Behandlerrolle.SAKSBEHANDLER,
                    brukerTilganger = brukerTilganger,
                ),
            )
        }
        if (grupper.contains(rolleConfig.veilederRolleKontantStøtte)) {
            brukerTilganger.putAll(
                hentTilgangMedRolle(
                    fagsystem = Tilgangskontrollsfagsystem.KONTANTSTØTTE,
                    behandlerrolle = Behandlerrolle.VEILEDER,
                    brukerTilganger = brukerTilganger,
                ),
            )
        }
        // forvalter har system tilgang
        if (grupper.contains(rolleConfig.forvalterRolleTeamfamilie)) {
            brukerTilganger.putAll(
                hentTilgangMedRolle(
                    fagsystem = Tilgangskontrollsfagsystem.FORVALTER_TILGANG,
                    behandlerrolle = Behandlerrolle.FORVALTER,
                    brukerTilganger = brukerTilganger,
                ),
            )
        }
        if (brukerTilganger.isEmpty()) {
            throw ForbiddenError(
                message = "Bruker har mangler tilgang til $handling",
                frontendFeilmelding = "Bruker har mangler tilgang til $handling",
                logContext = logContext,
            )
        }

        return InnloggetBrukertilgang(brukerTilganger.toMap())
    }

    fun erMaskinTilMaskinToken(): Boolean {
        val claims = SpringTokenValidationContextHolder().getTokenValidationContext().getClaims("azuread")
        return claims.get("oid") != null &&
            claims.get("oid") == claims.get("sub") &&
            claims.getAsList("roles").contains("access_as_application")
    }

    private fun hentTilgangMedRolle(
        fagsystem: Tilgangskontrollsfagsystem,
        behandlerrolle: Behandlerrolle,
        brukerTilganger: Map<Tilgangskontrollsfagsystem, Behandlerrolle>,
    ): Map<Tilgangskontrollsfagsystem, Behandlerrolle> {
        if (!harBrukerAlleredeHøyereTilgangPåSammeFagssystem(fagsystem, behandlerrolle, brukerTilganger)) {
            return mapOf(fagsystem to behandlerrolle)
        }
        return emptyMap()
    }

    private fun harBrukerAlleredeHøyereTilgangPåSammeFagssystem(
        fagsystem: Tilgangskontrollsfagsystem,
        behandlerrolle: Behandlerrolle,
        brukerTilganger: Map<Tilgangskontrollsfagsystem, Behandlerrolle>,
    ): Boolean {
        if (brukerTilganger.containsKey(fagsystem)) {
            return brukerTilganger[fagsystem]!!.nivå > behandlerrolle.nivå
        }
        return false
    }
}
