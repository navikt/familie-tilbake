package no.nav.tilbakekreving

enum class UtenforScope(val feilmelding: String) {
    KravgrunnlagIkkePerson("Kan ikke håndtere kravgrunnlag som ikke gjelder en person"),
    KravgrunnlagBrukerIkkeLikMottaker("Kravgrunnlag har mottaker som er ulik bruker"),
}
