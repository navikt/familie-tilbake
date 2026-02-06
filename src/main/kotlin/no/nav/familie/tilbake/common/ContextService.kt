package no.nav.familie.tilbake.common

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.common.exceptionhandler.ForbiddenError
import no.nav.familie.tilbake.config.Constants
import no.nav.familie.tilbake.log.SecureLog
import no.nav.familie.tilbake.sikkerhet.Behandlerrolle
import no.nav.familie.tilbake.sikkerhet.InnloggetBrukertilgang
import no.nav.familie.tilbake.sikkerhet.Tilgangskontrollsfagsystem
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.tilbakekreving.auth.Approlle
import no.nav.tilbakekreving.auth.Authentication
import no.nav.tilbakekreving.config.Tilgangsstyring
import no.nav.tilbakekreving.saksbehandler.Behandler

object ContextService {
    private const val SYSTEM_NAVN = "System"

    fun hentInnloggetBruker(): Authentication {
        val claims = SpringTokenValidationContextHolder().getTokenValidationContext().getClaims("azuread")
        val roles = claims.getAsList("roles")
        return when {
            erMaskinTilMaskinToken(claims) -> {
                Authentication.Systembruker(Approlle.roller(roles))
            }
            else -> Authentication.Ukjent
        }
    }

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
        tilgangsstyring: Tilgangsstyring,
        handling: String,
        logContext: SecureLog.Context,
    ): InnloggetBrukertilgang {
        val saksbehandler = hentSaksbehandler(logContext)
        val brukertilganger = mutableMapOf<Tilgangskontrollsfagsystem, Behandlerrolle>()
        if (saksbehandler == Constants.BRUKER_ID_VEDTAKSLØSNINGEN) {
            brukertilganger[Tilgangskontrollsfagsystem.SYSTEM_TILGANG] = Behandlerrolle.SYSTEM
        }
        val gruppeMedlemskap = hentGrupper()

        for ((fagsystem, grupper) in tilgangsstyring.grupper) {
            val tilgjengeligeRoller = grupper
                .filterValues { gruppeIder -> gruppeIder.any(gruppeMedlemskap::contains) }
                .keys
                .toMutableList()

            if (tilgangsstyring.forvalterGruppe in gruppeMedlemskap) {
                tilgjengeligeRoller += Behandlerrolle.FORVALTER
            }
            val rolle = tilgjengeligeRoller.maxByOrNull { it.nivå }

            if (rolle != null) {
                brukertilganger[Tilgangskontrollsfagsystem.fraFagsystem(fagsystem)] = rolle
            }
        }

        if (tilgangsstyring.forvalterGruppe in gruppeMedlemskap) {
            brukertilganger[Tilgangskontrollsfagsystem.FORVALTER_TILGANG] = Behandlerrolle.FORVALTER
        }

        if (brukertilganger.isEmpty()) {
            throw ForbiddenError(
                message = "Bruker har mangler tilgang til $handling",
                frontendFeilmelding = "Bruker har mangler tilgang til $handling",
                logContext = logContext,
            )
        }

        return InnloggetBrukertilgang(brukertilganger.toMap())
    }

    fun erMaskinTilMaskinToken(): Boolean {
        val claims = SpringTokenValidationContextHolder().getTokenValidationContext().getClaims("azuread")
        return erMaskinTilMaskinToken(claims)
    }

    fun erMaskinTilMaskinToken(claims: JwtTokenClaims): Boolean {
        return claims.get("oid") != null &&
            claims.get("oid") == claims.get("sub") &&
            claims.getAsList("roles").contains("access_as_application")
    }
}
