package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.kontrakter.Fagsystem
import no.nav.familie.tilbake.kontrakter.tilbakekreving.Ytelsestype

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
