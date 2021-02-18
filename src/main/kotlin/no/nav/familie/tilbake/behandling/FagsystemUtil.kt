package no.nav.familie.tilbake.behandling

import no.nav.familie.kontrakter.felles.tilbakekreving.Fagsystem
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype

object FagsystemUtil {

    fun hentFagsystemFraYtelsestype(type: Ytelsestype): Fagsystem {
        return when (type) {
            Ytelsestype.BARNETRYGD -> Fagsystem.BA
            Ytelsestype.KONTANTSTØTTE -> Fagsystem.KS
            Ytelsestype.OVERGANGSSTØNAD -> Fagsystem.EF
            Ytelsestype.BARNETILSYN -> Fagsystem.EF
            Ytelsestype.SKOLEPENGER -> Fagsystem.EF
        }
    }
}
