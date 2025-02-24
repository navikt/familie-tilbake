package no.nav.familie.tilbake.kontrakter.oppgave

data class FinnOppgaveResponseDto(
    val antallTreffTotalt: Long,
    val oppgaver: List<Oppgave>,
)
