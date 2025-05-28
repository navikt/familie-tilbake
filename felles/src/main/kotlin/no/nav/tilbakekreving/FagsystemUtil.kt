package no.nav.tilbakekreving

import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype

object FagsystemUtil {
    fun hentFagsystemFraYtelsestype(type: Ytelsestype): Fagsystem =
        when (type) {
            Ytelsestype.BARNETRYGD -> Fagsystem.BA
            Ytelsestype.KONTANTSTØTTE -> Fagsystem.KONT
            Ytelsestype.OVERGANGSSTØNAD -> Fagsystem.EF
            Ytelsestype.BARNETILSYN -> Fagsystem.EF
            Ytelsestype.SKOLEPENGER -> Fagsystem.EF
        }
}
