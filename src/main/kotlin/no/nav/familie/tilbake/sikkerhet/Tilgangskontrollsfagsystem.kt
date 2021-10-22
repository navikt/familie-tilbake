package no.nav.familie.tilbake.sikkerhet

import no.nav.familie.kontrakter.felles.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype

// Denne enum-en brukes kun for tilgangskontroll
enum class Tilgangskontrollsfagsystem(val kode: String) {

    BARNETRYGD("BA"),
    ENSLIG_FORELDER("EF"),
    KONTANTSTØTTE("KS"),
    FORVALTER_TILGANG("FT"), //brukes internt bare for tilgangsskontroll
    SYSTEM_TILGANG(""); //brukes internt bare for tilgangsskontroll

    companion object {

        fun fraYtelsestype(type: Ytelsestype): Tilgangskontrollsfagsystem {
            return when (type) {
                Ytelsestype.BARNETRYGD -> BARNETRYGD
                Ytelsestype.KONTANTSTØTTE -> KONTANTSTØTTE
                Ytelsestype.OVERGANGSSTØNAD -> ENSLIG_FORELDER
                Ytelsestype.BARNETILSYN -> ENSLIG_FORELDER
                Ytelsestype.SKOLEPENGER -> ENSLIG_FORELDER
            }
        }


        fun fraKode(kode: String): Tilgangskontrollsfagsystem {
            for (fagsystem in values()) {
                if (fagsystem.kode == kode) {
                    return fagsystem
                }
            }
            throw IllegalArgumentException("Fagsystem finnes ikke for kode $kode")
        }

        fun fraFagsystem(kontraktFagsystem: Fagsystem): Tilgangskontrollsfagsystem {
            for (fagsystem in values()) {
                if (fagsystem.kode == kontraktFagsystem.name) {
                    return fagsystem
                }
            }
            throw IllegalArgumentException("Fagsystem finnes ikke for kode $kontraktFagsystem")
        }
    }
}
