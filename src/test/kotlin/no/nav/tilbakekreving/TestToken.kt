package no.nav.tilbakekreving

import io.mockk.every
import io.mockk.mockk
import no.nav.security.token.support.core.context.TokenValidationContext

fun tokenValidationContextWith(value: String): TokenValidationContext = mockk {
    every { firstValidToken } returns mockk {
        every { encodedToken } returns value
    }
}
