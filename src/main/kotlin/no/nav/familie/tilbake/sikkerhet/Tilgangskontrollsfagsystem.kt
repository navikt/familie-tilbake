package no.nav.familie.tilbake.sikkerhet

import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO

// Denne enum-en brukes kun for tilgangskontroll
enum class Tilgangskontrollsfagsystem(
    val kode: String,
) {
    BARNETRYGD("BA"),
    ENSLIG_FORELDER("EF"),
    KONTANTSTØTTE("KONT"),
    TILLEGGSSTØNAD("TS"),
    FORVALTER_TILGANG("FT"), // brukes internt bare for tilgangsskontroll
    SYSTEM_TILGANG(""), // brukes internt bare for tilgangsskontroll
    ;

    companion object {
        fun fraKode(kode: String): Tilgangskontrollsfagsystem {
            for (fagsystem in values()) {
                if (fagsystem.kode == kode) {
                    return fagsystem
                }
            }
            throw IllegalArgumentException("Fagsystem finnes ikke for kode $kode")
        }

        fun fraFagsystem(kontraktFagsystem: FagsystemDTO): Tilgangskontrollsfagsystem {
            for (fagsystem in entries) {
                if (fagsystem.kode == kontraktFagsystem.name) {
                    return fagsystem
                }
            }
            throw IllegalArgumentException("Fagsystem finnes ikke for kode $kontraktFagsystem")
        }
    }
}
