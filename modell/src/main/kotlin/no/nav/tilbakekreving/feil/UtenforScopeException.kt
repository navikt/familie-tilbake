package no.nav.tilbakekreving.feil

import no.nav.tilbakekreving.UtenforScope

class UtenforScopeException(val utenforScope: UtenforScope) : Exception(utenforScope.feilmelding)
