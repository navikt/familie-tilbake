package no.nav.tilbakekreving

enum class UtenforScope(val tittel: String, val feilmelding: String) {
    KravgrunnlagIkkePerson(
        tittel = "Kravgrunnlaget er endret",
        feilmelding = "Kan ikke håndtere kravgrunnlag som ikke gjelder en person",
    ),
    KravgrunnlagBrukerIkkeLikMottaker(
        tittel = "Kravgrunnlaget er endret",
        feilmelding = "Kravgrunnlag har mottaker som er ulik bruker",
    ),
    KravgrunnlagStatusIkkeStøttet(
        tittel = "Kravgrunnlaget er endret",
        feilmelding = "Tilbakekrevingen kan ikke behandles videre fordi det har kommet endringer i feilutbetalingen/perioden fra fagsystemet eller økonomisystemet. Tilbakeløsningen har ikke støtte for å håndtere slike endringer enda, men vi jobber med å få dette på plass.",
    ),
    KravgrunnlagBortfalt(
        tittel = "Kravgrunnlaget er bortfalt",
        feilmelding = "Tilbakekrevingen kan ikke behandles videre fordi kravgrunnlaget kan være avsluttet i økonomisystemet.",
    ),
    KravgrunnlagMedFærrePerioder(
        tittel = "Kravgrunnlaget er endret",
        feilmelding = "Tilbakekrevingen kan ikke behandles videre fordi det har kommet endringer i feilutbetalingen/perioden fra fagsystemet eller økonomisystemet. Tilbakeløsningen har ikke støtte for å håndtere slike endringer enda, men vi jobber med å få dette på plass.",
    ),
    KravgrunnlagMedSammenslåttPerioder(
        tittel = "Kravgrunnlaget er endret",
        feilmelding = "Tilbakekrevingen kan ikke behandles videre fordi det har kommet endringer i feilutbetalingen/perioden fra fagsystemet eller økonomisystemet. Tilbakeløsningen har ikke støtte for å håndtere slike endringer enda, men vi jobber med å få dette på plass.",
    ),
    KravgrunnlagMedUlikeVerdier(
        tittel = "Kravgrunnlaget er endret",
        feilmelding = "Tilbakekrevingen kan ikke behandles videre fordi det har kommet endringer i feilutbetalingen/perioden fra fagsystemet eller økonomisystemet. Tilbakeløsningen har ikke støtte for å håndtere slike endringer enda, men vi jobber med å få dette på plass.",
    ),
    KravgrunnlagMedEndretBeløp(
        tittel = "Kravgrunnlaget er endret",
        feilmelding = "Tilbakekrevingen kan ikke behandles videre fordi det har kommet endringer i feilutbetalingen/perioden fra fagsystemet eller økonomisystemet. Tilbakeløsningen har ikke støtte for å håndtere slike endringer enda, men vi jobber med å få dette på plass.",
    ),
    KravgrunnlagSperret(
        tittel = "Kravgrunnlaget er sperret",
        feilmelding = "Tilbakekrevingen kan ikke behandles videre fordi kravgrunnlaget er sperret i økonomisystemet. Kravgrunnlaget kan endres eller avsluttes i økonomisystemet, og tilbakeløsningen har ikke støtte for å håndtere slike endringer enda, men vi jobber med å få dette på plass.",
    ),
    Revurdering(
        tittel = "Mangler funksjonalitet",
        feilmelding = "Revurdering er ikke støttet",
    ),
    PåVent(
        tittel = "Mangler funksjonalitet",
        feilmelding = "Kan ikke sette behandling på vent",
    ),
}
