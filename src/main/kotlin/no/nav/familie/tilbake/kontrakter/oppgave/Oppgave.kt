package no.nav.familie.tilbake.kontrakter.oppgave

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import jakarta.validation.constraints.Pattern
import no.nav.familie.tilbake.kontrakter.Tema

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
data class Oppgave(
    val id: Long? = null,
    val identer: List<OppgaveIdentV2>? = null,
    val tildeltEnhetsnr: String? = null,
    val endretAvEnhetsnr: String? = null,
    val opprettetAvEnhetsnr: String? = null,
    val journalpostId: String? = null,
    val journalpostkilde: String? = null,
    val behandlesAvApplikasjon: String? = null,
    val saksreferanse: String? = null,
    val bnr: String? = null,
    val samhandlernr: String? = null,
    @field:Pattern(regexp = "[0-9]{13}")
    val aktoerId: String? = null,
    @field:Pattern(regexp = "[0-9]{11,13}")
    val personident: String? = null,
    val orgnr: String? = null,
    val tilordnetRessurs: String? = null,
    val beskrivelse: String? = null,
    val temagruppe: String? = null,
    val tema: Tema? = null,
    val behandlingstema: String? = null,
    val oppgavetype: String? = null,
    val behandlingstype: String? = null,
    val versjon: Int? = null,
    val mappeId: Long? = null,
    val fristFerdigstillelse: String? = null,
    val aktivDato: String? = null,
    val opprettetTidspunkt: String? = null,
    val opprettetAv: String? = null,
    val endretAv: String? = null,
    val ferdigstiltTidspunkt: String? = null,
    val endretTidspunkt: String? = null,
    val prioritet: OppgavePrioritet? = null,
    val status: StatusEnum? = null,
    private var metadata: MutableMap<String, String>? = null,
)

enum class StatusEnum {
    OPPRETTET,
    AAPNET,
    UNDER_BEHANDLING,
    FERDIGSTILT,
    FEILREGISTRERT,
}
