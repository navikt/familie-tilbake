package no.nav.tilbakekreving.entities

import no.nav.tilbakekreving.brev.BrevInformasjon
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode

data class BrevInformasjonEntity(
    val brukerIdent: String,
    val brukerNavn: String,
    val mottaker: RegistrertBrevmottakerEntity,
    val behandlendeEnhet: EnhetEntity?,
    val ansvarligSaksbehandler: String,
    val saksnummer: String,
    val språkkode: Språkkode,
    val ytelse: YtelseEntity,
    val gjelderDødsfall: Boolean,
) {
    fun tilBrevinformasjon(): BrevInformasjon = BrevInformasjon(
        brukerIdent = brukerIdent,
        brukerNavn = brukerNavn,
        mottaker = mottaker.fraEntity(),
        behandlendeEnhet = behandlendeEnhet?.fraEntity(),
        ansvarligSaksbehandler = ansvarligSaksbehandler,
        saksnummer = saksnummer,
        språkkode = språkkode,
        ytelse = ytelse.fraEntity(),
        gjelderDødsfall = gjelderDødsfall,
    )
}
