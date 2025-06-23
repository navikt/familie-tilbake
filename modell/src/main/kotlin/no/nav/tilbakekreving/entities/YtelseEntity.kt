package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.fagsystem.Ytelse

data class YtelseEntity(
    val type: YtelseType,
) {
    fun fraEntity(): Ytelse {
        return when (type) {
            YtelseType.BARNETRYGD -> Ytelse.Barnetrygd
            YtelseType.TILLEGGSSTØNADER -> Ytelse.Tillegsstønader
        }
    }
}

enum class YtelseType {
    BARNETRYGD,
    TILLEGGSSTØNADER,
}
