package no.nav.tilbakekreving.fagsystem

import no.nav.tilbakekreving.entities.YtelseEntity
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.ytelse.DokarkivFagsaksystem
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kontrakter.ytelse.Tema
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO
import kotlin.collections.mapOf

sealed interface Ytelse {
    fun tilFagsystemDTO(): FagsystemDTO

    fun tilYtelseDTO(): YtelsestypeDTO

    fun integrererMotFagsystem(): Boolean

    fun tilYtelsestype(): Ytelsestype

    fun tilDokarkivFagsaksystem(): DokarkivFagsaksystem

    fun tilTema(): Tema

    val kafkaTopic: String

    fun tilEntity(): YtelseEntity

    object Barnetrygd : Ytelse {
        override fun tilFagsystemDTO(): FagsystemDTO = FagsystemDTO.BA

        override fun tilYtelseDTO(): YtelsestypeDTO = YtelsestypeDTO.BARNETRYGD

        override fun integrererMotFagsystem(): Boolean = true

        override fun tilYtelsestype(): Ytelsestype = Ytelsestype.BARNETRYGD

        override fun tilTema(): Tema = Tema.BAR

        override fun tilDokarkivFagsaksystem(): DokarkivFagsaksystem = DokarkivFagsaksystem.BA

        override val kafkaTopic: String = "tilbake.privat-tilbakekreving-barnetrygd"

        override fun tilEntity(): YtelseEntity = YtelseEntity(Ytelsestype.BARNETRYGD)
    }

    object Tilleggsstønad : Ytelse {
        override fun tilFagsystemDTO(): FagsystemDTO = FagsystemDTO.TS

        override fun tilYtelseDTO(): YtelsestypeDTO = YtelsestypeDTO.TILLEGGSSTØNAD

        override fun integrererMotFagsystem(): Boolean = true

        override fun tilYtelsestype(): Ytelsestype = Ytelsestype.TILLEGGSSTØNAD

        override fun tilTema(): Tema = Tema.TSO

        override fun tilDokarkivFagsaksystem(): DokarkivFagsaksystem = DokarkivFagsaksystem.TILLEGGSSTONADER

        override val kafkaTopic: String = "tilbake.privat-tilbakekreving-tilleggsstonad"

        override fun tilEntity(): YtelseEntity = YtelseEntity(Ytelsestype.TILLEGGSSTØNAD)
    }

    object Arbeidsavklaringspenger : Ytelse {
        override fun tilFagsystemDTO(): FagsystemDTO = FagsystemDTO.AAP

        override fun tilYtelseDTO(): YtelsestypeDTO = YtelsestypeDTO.ARBEIDSAVKLARINGSPENGER

        override fun integrererMotFagsystem(): Boolean = false

        override fun tilYtelsestype(): Ytelsestype = Ytelsestype.ARBEIDSAVKLARINGSPENGER

        override fun tilTema(): Tema = Tema.AAP

        override fun tilDokarkivFagsaksystem(): DokarkivFagsaksystem = DokarkivFagsaksystem.KELVIN

        override val kafkaTopic: String = "tilbake.privat-tilbakekreving-arbeidsavklaringspenger"

        override fun tilEntity(): YtelseEntity = YtelseEntity(Ytelsestype.ARBEIDSAVKLARINGSPENGER)
    }

    companion object {
        fun ytelser() = setOf(
            Tilleggsstønad,
        )
    }
}

enum class Ytelsestype(val kode: String, val navn: Map<Språkkode, String>) {
    BARNETRYGD(
        "BA",
        mapOf(
            Språkkode.NB to "Barnetrygd",
            Språkkode.NN to "Barnetrygd",
        ),
    ),
    TILLEGGSSTØNAD(
        "TSO",
        mapOf(
            Språkkode.NB to "Tilleggsstønad",
            Språkkode.NN to "Tilleggsstønad",
        ),
    ),
    KONTANTSTØTTE(
        "KS",
        mapOf(
            Språkkode.NB to "Kontantstøtte",
            Språkkode.NN to "Kontantstøtte",
        ),
    ),
    OVERGANGSSTØNAD(
        "EF",
        mapOf(
            Språkkode.NB to "Overgangsstønad",
            Språkkode.NN to "Overgangsstønad",
        ),
    ),
    INFOTRYGD(
        "IT01",
        mapOf(
            Språkkode.NB to "Infotrygd",
            Språkkode.NN to "Infotrygd",
        ),
    ),
    ARBEIDSAVKLARINGSPENGER(
        "AAP",
        mapOf(
            Språkkode.NB to "Arbeidsavklaringspenger",
            Språkkode.NN to "Arbeidsavklaringspengar",
        ),
    ),
}
