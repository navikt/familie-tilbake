package no.nav.tilbakekreving.e2e

import io.jsonwebtoken.Jwts
import no.nav.security.token.support.core.context.TokenValidationContext
import no.nav.security.token.support.core.jwt.JwtToken
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.tilbakekreving.test.FellesTestdata.SAKSBEHANDLER_IDENT
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestAttributes
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date

object ContextServiceHelpers {
    const val E2E_TILGANG_GRUPPE = "e2e-tilgang"

    fun <T> somSaksbehandler(
        ident: String = SAKSBEHANDLER_IDENT,
        grupper: List<String> = listOf(E2E_TILGANG_GRUPPE),
        block: () -> T,
    ): T {
        val tidligereRequestAttributes = RequestContextHolder.getRequestAttributes()
        try {
            RequestContextHolder.setRequestAttributes(ServletRequestAttributes(MockHttpServletRequest()))
            RequestContextHolder
                .currentRequestAttributes()
                .setAttribute(
                    SpringTokenValidationContextHolder::class.java.name,
                    tokenValidationContext(ident, grupper),
                    RequestAttributes.SCOPE_REQUEST,
                )
            return block()
        } finally {
            RequestContextHolder.setRequestAttributes(tidligereRequestAttributes)
        }
    }

    fun tokenValidationContext(
        ident: String,
        grupper: List<String>,
    ): TokenValidationContext = TokenValidationContext(
        mapOf(
            "azuread" to JwtToken(
                Jwts
                    .builder()
                    .expiration(Date.from(Instant.now().plus(5, ChronoUnit.MINUTES)))
                    .issuer("azuread")
                    .claims(
                        mapOf(
                            "NAVident" to ident,
                            "groups" to grupper,
                            "roles" to emptyList<String>(),
                        ),
                    )
                    .compact(),
            ),
        ),
    )
}
