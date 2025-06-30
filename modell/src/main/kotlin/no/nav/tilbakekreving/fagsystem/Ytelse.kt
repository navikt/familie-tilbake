package no.nav.tilbakekreving.fagsystem

import no.nav.tilbakekreving.entities.YtelseEntity
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO

sealed interface Ytelse {
    fun tilFagsystemDTO(): FagsystemDTO

    fun tilYtelseDTO(): YtelsestypeDTO

    fun integrererMotFagsystem(): Boolean

    fun tilYtelsestype(): Ytelsestype

    fun tilEntity(): YtelseEntity

    object Barnetrygd : Ytelse {
        override fun tilFagsystemDTO(): FagsystemDTO = FagsystemDTO.BA

        override fun tilYtelseDTO(): YtelsestypeDTO = YtelsestypeDTO.BARNETRYGD

        override fun integrererMotFagsystem(): Boolean = true

        override fun tilYtelsestype(): Ytelsestype = Ytelsestype.BARNETRYGD

        override fun tilEntity(): YtelseEntity = YtelseEntity(Ytelsestype.BARNETRYGD)
    }

    object Tilleggsstønad : Ytelse {
        override fun tilFagsystemDTO(): FagsystemDTO = FagsystemDTO.TS

        override fun tilYtelseDTO(): YtelsestypeDTO = YtelsestypeDTO.TILLEGGSSTØNAD

        override fun integrererMotFagsystem(): Boolean = false

        override fun tilYtelsestype(): Ytelsestype = Ytelsestype.TILLEGGSSTØNAD

        override fun tilEntity(): YtelseEntity = YtelseEntity(Ytelsestype.TILLEGGSSTØNAD)
    }
}

enum class Ytelsestype(val kode: String) {
    BARNETRYGD("BA"),
    TILLEGGSSTØNAD("TSO"),
}
