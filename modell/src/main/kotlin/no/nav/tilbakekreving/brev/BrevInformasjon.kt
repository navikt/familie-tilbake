package no.nav.tilbakekreving.brev

import no.nav.tilbakekreving.behandling.Enhet
import no.nav.tilbakekreving.behandling.saksbehandling.RegistrertBrevmottaker
import no.nav.tilbakekreving.entities.BrevInformasjonEntity
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode

data class BrevInformasjon(
    val brukerIdent: String,
    val brukerNavn: String,
    val mottaker: RegistrertBrevmottaker,
    val behandlendeEnhet: Enhet?,
    val ansvarligSaksbehandler: String,
    val saksnummer: String,
    val språkkode: Språkkode,
    val ytelse: Ytelse,
    val gjelderDødsfall: Boolean,
) {
    fun tilEntity(): BrevInformasjonEntity {
        return BrevInformasjonEntity(
            brukerIdent = brukerIdent,
            brukerNavn = brukerNavn,
            mottaker = mottaker.tilEntity(),
            behandlendeEnhet = behandlendeEnhet?.tilEntity(),
            ansvarligSaksbehandler = ansvarligSaksbehandler,
            saksnummer = saksnummer,
            språkkode = språkkode,
            ytelse = ytelse.tilEntity(),
            gjelderDødsfall = gjelderDødsfall,
        )
    }
}
