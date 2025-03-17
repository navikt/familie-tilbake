package no.nav.familie.tilbake.kontrakter.oppgave

import no.nav.tilbakekreving.kontrakter.ytelse.Tema
import java.time.LocalDate

data class OpprettOppgaveRequest(
    val ident: OppgaveIdentV2?,
    val enhetsnummer: String?,
    val saksId: String?,
    val tema: Tema,
    val oppgavetype: Oppgavetype,
    val behandlingstema: String?,
    val tilordnetRessurs: String? = null,
    val fristFerdigstillelse: LocalDate,
    val beskrivelse: String,
    val prioritet: OppgavePrioritet = OppgavePrioritet.NORM,
    val behandlingstype: String? = null,
    val behandlesAvApplikasjon: String? = null,
    val mappeId: Long? = null,
)

enum class Oppgavetype(
    val value: String,
) {
    BehandleSak("BEH_SAK"),
    GodkjenneVedtak("GOD_VED"),
    BehandleUnderkjentVedtak("BEH_UND_VED"),
    VurderHenvendelse("VURD_HENV"),
}

enum class Behandlingstype(
    val value: String,
) {
    Tilbakekreving("ae0161"),
}

enum class OppgavePrioritet {
    HOY,
    NORM,
    LAV,
}
