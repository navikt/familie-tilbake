package no.nav.tilbakekreving

import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO

object FagsystemUtil {
    fun hentFagsystemFraYtelsestype(type: YtelsestypeDTO): FagsystemDTO =
        when (type) {
            YtelsestypeDTO.BARNETRYGD -> FagsystemDTO.BA
            YtelsestypeDTO.KONTANTSTØTTE -> FagsystemDTO.KONT
            YtelsestypeDTO.OVERGANGSSTØNAD -> FagsystemDTO.EF
            YtelsestypeDTO.BARNETILSYN -> FagsystemDTO.EF
            YtelsestypeDTO.SKOLEPENGER -> FagsystemDTO.EF
            YtelsestypeDTO.TILLEGGSTØNADER -> FagsystemDTO.TS
        }
}
