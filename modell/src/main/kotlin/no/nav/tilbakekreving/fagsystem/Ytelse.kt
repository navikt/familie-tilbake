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
        override fun tilFagsystemDTO(): FagsystemDTO = FagsystemDTO.TP

        override fun tilYtelseDTO(): YtelsestypeDTO = YtelsestypeDTO.TILTAKSPENGER

        override fun integrererMotFagsystem(): Boolean = true

        override fun tilYtelsestype(): Ytelsestype = Ytelsestype.TILTAKSPENGER

        override fun tilTema(): Tema = Tema.IND

        override fun hentYtelsesnavn(språkkode: Språkkode): String {
            return when (språkkode) {
                Språkkode.NB -> "Tiltakspenger"
                Språkkode.NN -> "Tiltakspengar"
            }
        }

        override fun tilDokarkivFagsaksystem(): DokarkivFagsaksystem = DokarkivFagsaksystem.TILTAKSPENGER

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

    object Dagpenger : Ytelse {
        override fun tilFagsystemDTO(): FagsystemDTO = FagsystemDTO.DP

        override fun tilYtelseDTO(): YtelsestypeDTO = YtelsestypeDTO.DAGPENGER

        override fun integrererMotFagsystem(): Boolean = true

        override fun tilYtelsestype(): Ytelsestype = Ytelsestype.DAGPENGER

        override fun tilTema(): Tema = Tema.DAG

        override fun hentYtelsesnavn(språkkode: Språkkode): String {
            return when (språkkode) {
                Språkkode.NB -> "Dagpenger"
                Språkkode.NN -> "Dagpengar"
            }
        }

        override fun tilDokarkivFagsaksystem(): DokarkivFagsaksystem = DokarkivFagsaksystem.DAGPENGER

        override val kafkaTopic: String = "tilbake.privat-tilbakekreving-dagpenger"

        override fun tilEntity(): YtelseEntity = YtelseEntity(Ytelsestype.DAGPENGER)

        override fun brevmeta(): YtelseDto {
            return YtelseDto(
                url = "nav.no/dagpenger",
                ubestemtEntall = "dagpenger",
                bestemtEntall = "dagpengene",
            )
        }
    }

    companion object {
        fun ytelser() = setOf(
            Tilleggsstønad,
            Arbeidsavklaringspenger,
            Tiltakspenger,
            Dagpenger,
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
    DAGPENGER("DAGPENGER"),
}
