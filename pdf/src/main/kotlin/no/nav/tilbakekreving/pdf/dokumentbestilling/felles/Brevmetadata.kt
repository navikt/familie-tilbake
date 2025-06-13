package no.nav.tilbakekreving.pdf.dokumentbestilling.felles

import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.kontrakter.ytelse.YtelsestypeDTO
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.header.Institusjon
import no.nav.tilbakekreving.pdf.handlebars.dto.Språkstøtte

data class Brevmetadata(
    val sakspartId: String,
    val sakspartsnavn: String,
    val finnesVerge: Boolean = false,
    val finnesAnnenMottaker: Boolean = finnesVerge,
    val vergenavn: String? = null,
    val annenMottakersNavn: String? = null,
    val mottageradresse: Adresseinfo,
    val behandlendeEnhetId: String? = null,
    val behandlendeEnhetsNavn: String,
    val ansvarligSaksbehandler: String,
    val saksnummer: String,
    override val språkkode: Språkkode,
    val ytelsestype: YtelsestypeDTO,
    val behandlingstype: Behandlingstype? = null,
    val tittel: String? = null,
    val gjelderDødsfall: Boolean,
    val institusjon: Institusjon? = null,
) : Språkstøtte {
    init {
        if (finnesAnnenMottaker && !finnesVerge) {
            requireNotNull(annenMottakersNavn) { "annenMottakersNavn kan ikke være null" }
        }
    }
}
