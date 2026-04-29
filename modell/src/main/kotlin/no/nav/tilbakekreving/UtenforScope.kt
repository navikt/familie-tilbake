package no.nav.tilbakekreving

enum class UtenforScope(val feilmelding: String) {
    KravgrunnlagIkkePerson("Kan ikke håndtere kravgrunnlag som ikke gjelder en person"),
    KravgrunnlagBrukerIkkeLikMottaker("Kravgrunnlag har mottaker som er ulik bruker"),
    KravgrunnlagStatusIkkeStøttet("Tilbakekrevingen kan ikke behandles videre fordi det har kommet endringer i feilutbetalingen/perioden fra fagsystemet eller økonomisystemet. Tilbakeløsningen har ikke støtte for å håndtere slike endringer enda, men vi jobber med å få dette på plass."),
    KravgrunnlagStatusIkkeStøttetEtterBehandlingenErPåbegynt("Tilbakekrevingen kan ikke behandles videre fordi det har kommet endringer i feilutbetalingen/perioden fra fagsystemet eller økonomisystemet. Tilbakeløsningen har ikke støtte for å håndtere slike endringer enda, men vi jobber med å få dette på plass."),
    Revurdering("Revurdering er ikke støttet"),
}
