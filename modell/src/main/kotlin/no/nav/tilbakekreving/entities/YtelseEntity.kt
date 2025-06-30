package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.fagsystem.Ytelsestype

data class YtelseEntity(
    val type: Ytelsestype,
) {
    fun fraEntity(): Ytelse {
        return when (type) {
            Ytelsestype.BARNETRYGD -> Ytelse.Barnetrygd
            Ytelsestype.TILLEGGSSTØNAD -> Ytelse.Tilleggsstønad
        }
    }
}
