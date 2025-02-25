package no.nav.familie.tilbake.kontrakter.saksbehandler

import java.util.UUID

class Saksbehandler(
    val azureId: UUID,
    val navIdent: String,
    val fornavn: String,
    val etternavn: String,
    val enhet: String,
)
