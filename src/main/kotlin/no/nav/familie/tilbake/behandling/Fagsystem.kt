package no.nav.familie.tilbake.behandling

import no.nav.familie.tilbake.common.exceptionhandler.Feil
import no.nav.familie.tilbake.log.SecureLog
import no.nav.tilbakekreving.kontrakter.ytelse.DokarkivFagsaksystem
import no.nav.tilbakekreving.kontrakter.ytelse.FagsystemDTO
import org.springframework.http.HttpStatus

enum class Fagsystem(
    val navn: String,
    val tema: String,
) {
    BA("Barnetrygd", "BAR"),
    EF("Enslig forelder", "ENF"),

    @Deprecated(message = "Gyldig verdi for kontantstøtte er KONT")
    KS("Kontantstøtte - gammel", "KON"),

    KONT("Kontantstøtte", "KON"),
    IT01("Infotrygd", ""),
    ;

    fun tilDTO() = when (this) {
        BA -> FagsystemDTO.BA
        EF -> FagsystemDTO.EF
        KS, KONT -> FagsystemDTO.KONT
        IT01 -> FagsystemDTO.IT01
    }

    fun tilDokarkivFagsaksystem() = when (this) {
        BA -> DokarkivFagsaksystem.BA
        EF -> DokarkivFagsaksystem.EF
        KS, KONT -> DokarkivFagsaksystem.KONT
        IT01 -> DokarkivFagsaksystem.IT01
    }

    companion object {
        fun forDTO(fagsystem: FagsystemDTO) = when (fagsystem) {
            FagsystemDTO.BA -> BA
            FagsystemDTO.EF -> EF
            FagsystemDTO.KONT -> KONT
            FagsystemDTO.IT01 -> IT01
            else -> throw Feil("Kan ikke håndtere ytelser fra $fagsystem med gammel modell", logContext = SecureLog.Context.tom(), httpStatus = HttpStatus.BAD_REQUEST)
        }
    }
}
