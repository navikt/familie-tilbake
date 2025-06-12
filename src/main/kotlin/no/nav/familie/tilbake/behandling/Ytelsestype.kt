package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.ytelse.Tema
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO
import org.springframework.http.HttpStatus

enum class Ytelsestype(
    val kode: String,
    val navn: Map<Språkkode, String>,
) {
    BARNETRYGD(
        "BA",
        mapOf(
            Språkkode.NB to "Barnetrygd",
            Språkkode.NN to "Barnetrygd",
        ),
    ),
    OVERGANGSSTØNAD(
        "EFOG",
        mapOf(
            Språkkode.NB to "Overgangsstønad",
            Språkkode.NN to "Overgangsstønad",
        ),
    ),
    BARNETILSYN(
        "EFBT",
        mapOf(
            Språkkode.NB to "Stønad til barnetilsyn",
            Språkkode.NN to "Stønad til barnetilsyn",
        ),
    ),
    SKOLEPENGER(
        "EFSP",
        mapOf(
            Språkkode.NB to "Stønad til skolepenger",
            Språkkode.NN to "Stønad til skulepengar",
        ),
    ),
    KONTANTSTØTTE(
        "KS",
        mapOf(
            Språkkode.NB to "Kontantstøtte",
            Språkkode.NN to "Kontantstøtte",
        ),
    ),
    ;

    fun tilTema(): Tema =
        when (this) {
            BARNETRYGD -> Tema.BAR
            BARNETILSYN, OVERGANGSSTØNAD, SKOLEPENGER -> Tema.ENF
            KONTANTSTØTTE -> Tema.KON
        }

    fun tilDTO(): YtelsestypeDTO {
        return when (this) {
            BARNETRYGD -> YtelsestypeDTO.BARNETRYGD
            OVERGANGSSTØNAD -> YtelsestypeDTO.OVERGANGSSTØNAD
            BARNETILSYN -> YtelsestypeDTO.BARNETILSYN
            SKOLEPENGER -> YtelsestypeDTO.SKOLEPENGER
            KONTANTSTØTTE -> YtelsestypeDTO.KONTANTSTØTTE
        }
    }

    companion object {
        fun forDTO(ytelsestypeDTO: YtelsestypeDTO): Ytelsestype {
            return when (ytelsestypeDTO) {
                YtelsestypeDTO.BARNETRYGD -> BARNETRYGD
                YtelsestypeDTO.OVERGANGSSTØNAD -> OVERGANGSSTØNAD
                YtelsestypeDTO.BARNETILSYN -> BARNETILSYN
                YtelsestypeDTO.SKOLEPENGER -> SKOLEPENGER
                YtelsestypeDTO.KONTANTSTØTTE -> KONTANTSTØTTE
                else -> throw Feil("Kan ikke håndtere ytelser for $ytelsestypeDTO med gammel modell", logContext = SecureLog.Context.tom(), httpStatus = HttpStatus.BAD_REQUEST)
            }
        }
    }
}
