package no.nav.tilbakekreving.behov

import no.nav.tilbakekreving.brev.Varselbrev
import java.util.UUID

data class VarselbrevBehov(
    val brevId: UUID,
    val varselbrev: Varselbrev,
) : Behov
