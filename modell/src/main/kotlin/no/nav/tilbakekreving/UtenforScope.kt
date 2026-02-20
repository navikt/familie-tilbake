package no.nav.tilbakekreving

enum class UtenforScope(val feilmelding: String) {
    KravgrunnlagIkkePerson("Kan ikke håndtere kravgrunnlag som ikke gjelder en person"),
    KravgrunnlagBrukerIkkeLikMottaker("Kravgrunnlag har mottaker som er ulik bruker"),
    KravgrunnlagStatusIkkeStøttet("Støtter kun kravgrunnlag med status kode NY og ENDR"),
    KravgrunnlagStatusIkkeStøttetEtterBehandlingenErPåbegynt("Støtter ikke kravgrunnlag med status ENDR når behandlingen er påbegynt"),
    Revurdering("Revurdering er ikke støttet"),
}
