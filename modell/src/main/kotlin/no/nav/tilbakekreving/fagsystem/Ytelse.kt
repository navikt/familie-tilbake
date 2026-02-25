package no.nav.tilbakekreving.fagsystem

import no.nav.tilbakekreving.entities.YtelseEntity
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.frontend.models.YtelseDto
import no.nav.tilbakekreving.kontrakter.ytelse.DokarkivFagsaksystem
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kontrakter.ytelse.Tema
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO

sealed interface Ytelse {
    fun tilFagsystemDTO(): FagsystemDTO

    fun tilYtelseDTO(): YtelsestypeDTO

    fun integrererMotFagsystem(): Boolean

    fun tilYtelsestype(): Ytelsestype

    fun tilDokarkivFagsaksystem(): DokarkivFagsaksystem

    fun tilTema(): Tema

    fun hentYtelsesnavn(språkkode: Språkkode): String

    val kafkaTopic: String

    fun tilEntity(): YtelseEntity

    fun brevmeta(): YtelseDto

    object Barnetrygd : Ytelse {
        override fun tilFagsystemDTO(): FagsystemDTO = FagsystemDTO.BA

        override fun tilYtelseDTO(): YtelsestypeDTO = YtelsestypeDTO.BARNETRYGD

        override fun integrererMotFagsystem(): Boolean = true

        override fun tilYtelsestype(): Ytelsestype = Ytelsestype.BARNETRYGD

        override fun tilTema(): Tema = Tema.BAR

        override fun hentYtelsesnavn(språkkode: Språkkode): String {
            return when (språkkode) {
                Språkkode.NB -> "Barnetrygd"
                Språkkode.NN -> "Barnetrygd"
            }
        }

        override fun tilDokarkivFagsaksystem(): DokarkivFagsaksystem = DokarkivFagsaksystem.BA

        override val kafkaTopic: String = "tilbake.privat-tilbakekreving-barnetrygd"

        override fun tilEntity(): YtelseEntity = YtelseEntity(Ytelsestype.BARNETRYGD)

        override fun brevmeta(): YtelseDto {
            return YtelseDto(
                url = "nav.no/barnetrygd",
                ubestemtEntall = "barnetrygd",
                bestemtEntall = "barnetrygden",
            )
        }
    }

    object Tilleggsstønad : Ytelse {
        override fun tilFagsystemDTO(): FagsystemDTO = FagsystemDTO.TS

        override fun tilYtelseDTO(): YtelsestypeDTO = YtelsestypeDTO.TILLEGGSSTØNAD

        override fun integrererMotFagsystem(): Boolean = true

        override fun tilYtelsestype(): Ytelsestype = Ytelsestype.TILLEGGSSTØNAD

        override fun tilTema(): Tema = Tema.TSO

        override fun hentYtelsesnavn(språkkode: Språkkode): String {
            return when (språkkode) {
                Språkkode.NB -> "Tilleggsstønad"
                Språkkode.NN -> "Tilleggsstønad"
            }
        }

        override fun tilDokarkivFagsaksystem(): DokarkivFagsaksystem = DokarkivFagsaksystem.TILLEGGSSTONADER

        override val kafkaTopic: String = "tilbake.privat-tilbakekreving-tilleggsstonad"

        override fun tilEntity(): YtelseEntity = YtelseEntity(Ytelsestype.TILLEGGSSTØNAD)

        override fun brevmeta(): YtelseDto {
            return YtelseDto(
                url = "nav.no/tilleggsstonader",
                ubestemtEntall = "tilleggsstønad",
                bestemtEntall = "tilleggsstønaden",
            )
        }
    }

    object Arbeidsavklaringspenger : Ytelse {
        override fun tilFagsystemDTO(): FagsystemDTO = FagsystemDTO.AAP

        override fun tilYtelseDTO(): YtelsestypeDTO = YtelsestypeDTO.ARBEIDSAVKLARINGSPENGER

        override fun integrererMotFagsystem(): Boolean = false

        override fun tilYtelsestype(): Ytelsestype = Ytelsestype.ARBEIDSAVKLARINGSPENGER

        override fun tilTema(): Tema = Tema.AAP

        override fun hentYtelsesnavn(språkkode: Språkkode): String {
            return when (språkkode) {
                Språkkode.NB -> "Arbeidsavklaringspenger"
                Språkkode.NN -> "Arbeidsavklaringspengar"
            }
        }

        override fun tilDokarkivFagsaksystem(): DokarkivFagsaksystem = DokarkivFagsaksystem.KELVIN

        override val kafkaTopic: String = "tilbake.privat-tilbakekreving-arbeidsavklaringspenger"

        override fun tilEntity(): YtelseEntity = YtelseEntity(Ytelsestype.ARBEIDSAVKLARINGSPENGER)

        override fun brevmeta(): YtelseDto {
            return YtelseDto(
                url = "nav.no/aap",
                ubestemtEntall = "arbeidsavklaringspenger",
                bestemtEntall = "arbeidsavklaringspengene",
            )
        }
    }

    object Tiltakspenger : Ytelse {
        override fun tilFagsystemDTO(): FagsystemDTO = FagsystemDTO.TILTAKSPENGER

        override fun tilYtelseDTO(): YtelsestypeDTO = YtelsestypeDTO.TILTAKSPENGER

        override fun integrererMotFagsystem(): Boolean = false

        override fun tilYtelsestype(): Ytelsestype = Ytelsestype.TILTAKSPENGER

        override fun tilTema(): Tema = Tema.IND

        override fun hentYtelsesnavn(språkkode: Språkkode): String {
            return when (språkkode) {
                Språkkode.NB -> "Tiltakspenger"
                Språkkode.NN -> "Tiltakspengar"
            }
        }

        override fun tilDokarkivFagsaksystem(): DokarkivFagsaksystem = DokarkivFagsaksystem.KELVIN

        override val kafkaTopic: String = "tilbake.privat-tilbakekreving-tiltakspenger"

        override fun tilEntity(): YtelseEntity = YtelseEntity(Ytelsestype.TILTAKSPENGER)

        override fun brevmeta(): YtelseDto {
            return YtelseDto(
                url = "nav.no/tiltakspenger",
                ubestemtEntall = "tiltakspenger",
                bestemtEntall = "tiltakspengene",
            )
        }
    }

    companion object {
        fun ytelser() = setOf(
            Tilleggsstønad,
            Arbeidsavklaringspenger,
            Tiltakspenger,
        )
    }
}

enum class Ytelsestype(val kode: String) {
    BARNETRYGD("BA"),
    TILLEGGSSTØNAD("TSO"),
    KONTANTSTØTTE("KS"),
    OVERGANGSSTØNAD("EF"),
    INFOTRYGD("IT01"),
    ARBEIDSAVKLARINGSPENGER("AAP"),
    TILTAKSPENGER("TILTAKSPENGER"),
}
