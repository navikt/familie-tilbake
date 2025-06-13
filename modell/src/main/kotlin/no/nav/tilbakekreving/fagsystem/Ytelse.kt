package no.nav.tilbakekreving.fagsystem

import no.nav.tilbakekreving.entities.YtelseEntity
import no.nav.tilbakekreving.entities.YtelseType
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO

sealed interface Ytelse {
    fun tilFagsystemDTO(): FagsystemDTO

    fun tilYtelseDTO(): YtelsestypeDTO

    fun integrererMotFagsystem(): Boolean

    fun tilEntity(): YtelseEntity

    object Barnetrygd : Ytelse {
        override fun tilFagsystemDTO(): FagsystemDTO = FagsystemDTO.BA

        override fun tilYtelseDTO(): YtelsestypeDTO = YtelsestypeDTO.BARNETRYGD

        override fun integrererMotFagsystem(): Boolean = true

        override fun tilEntity(): YtelseEntity = YtelseEntity(YtelseType.BARNETRYGD)
    }

    object Tillegsstønader : Ytelse {
        override fun tilFagsystemDTO(): FagsystemDTO = FagsystemDTO.TS

        override fun tilYtelseDTO(): YtelsestypeDTO = YtelsestypeDTO.TILLEGGSTØNADER

        override fun integrererMotFagsystem(): Boolean = false

        override fun tilEntity(): YtelseEntity = YtelseEntity(YtelseType.TILLEGGSSTØNADER)
    }
}
