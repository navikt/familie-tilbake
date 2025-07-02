package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.fagsystem.Ytelse

data class YtelseEntity(
    val type: YtelseType,
) {
    fun fraEntity(): Ytelse {
        return when (type) {
            YtelseType.BARNETRYGD -> Ytelse.Barnetrygd
            YtelseType.TILLEGGSSTØNAD -> Ytelse.Tilleggsstønad
        }
    }
}

enum class YtelseType {
    BARNETRYGD,
    TILLEGGSSTØNAD,
}
