package no.nav.familie.tilbake.kontrakter.oppgave

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.tilbakekreving.kontrakter.ytelse.Tema

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Oppgave(
    val id: Long? = null,
    val tildeltEnhetsnr: String? = null,
    val tilordnetRessurs: String? = null,
    val beskrivelse: String? = null,
    val tema: Tema? = null,
    val oppgavetype: String? = null,
    val fristFerdigstillelse: String? = null,
    val opprettetTidspunkt: String? = null,
    val prioritet: OppgavePrioritet? = null,
    val status: StatusEnum? = null,
)

enum class StatusEnum {
    OPPRETTET,
    AAPNET,
    UNDER_BEHANDLING,
    FERDIGSTILT,
    FEILREGISTRERT,
}
