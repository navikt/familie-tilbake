package no.nav.familie.tilbake.kontrakter.oppgave

data class OppgaveIdentV2(
    val ident: String?,
    val gruppe: IdentGruppe?,
)

enum class IdentGruppe {
    AKTOERID,
}
