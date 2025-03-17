package no.nav.familie.tilbake.kontrakter.oppgave

import no.nav.tilbakekreving.kontrakter.ytelse.Tema

data class FinnOppgaveRequest(
    val tema: Tema,
    val oppgavetype: Oppgavetype? = null,
    val saksreferanse: String? = null,
)
