package no.nav.tilbakekreving.brev.varselbrev

import no.nav.familie.tilbake.config.Constants
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.kontrakter.bruker.Språkkode
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Adresseinfo
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata
import no.nav.tilbakekreving.pdf.dokumentbestilling.varsel.TekstformatererVarselbrev
import no.nav.tilbakekreving.pdf.dokumentbestilling.varsel.handlebars.dto.Varselbrevsdokument
import org.springframework.stereotype.Service

@Service
class ForhåndsvarselService() {
    fun hentVarselbrevTekster(tilbakekreving: Tilbakekreving): List<Section> {
        val brevmetadata = opprettMetadata(tilbakekreving)
        val overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(brevmetadata, false)
        val brevbody = TekstformatererVarselbrev.lagFritekst(opprettVarselbrevsdokument(tilbakekreving, brevmetadata), false)

        val varselbrevAvsnitter = VarselbrevParser.parse(brevbody)
        varselbrevAvsnitter.add(0, Section("overskrift", overskrift))
        return varselbrevAvsnitter
    }

    private fun opprettMetadata(tilbakekreving: Tilbakekreving): Brevmetadata {
        val bruker = tilbakekreving.bruker!!.tilFrontendDto()
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry
        return Brevmetadata(
            sakspartId = bruker.personIdent,
            sakspartsnavn = bruker.navn,
            mottageradresse = Adresseinfo(bruker.personIdent, bruker.navn),
            behandlendeEnhetsNavn = behandling.hentBehandlingsinformasjon().enhet?.navn ?: "Ukjent", // Todo fjern Ukjent når enhet er på plass
            ansvarligSaksbehandler = behandling.hentBehandlingsinformasjon().ansvarligSaksbehandler.ident,
            saksnummer = tilbakekreving.eksternFagsak.eksternId,
            språkkode = tilbakekreving.bruker!!.språkkode ?: Språkkode.NB,
            ytelsestype = tilbakekreving.hentFagsysteminfo().tilYtelseDTO(),
            gjelderDødsfall = bruker.dødsdato != null,
        )
    }

    private fun opprettVarselbrevsdokument(tilbakekreving: Tilbakekreving, brevmetadata: Brevmetadata): Varselbrevsdokument {
        val behandling = tilbakekreving.behandlingHistorikk.nåværende().entry
        return Varselbrevsdokument(
            brevmetadata = brevmetadata,
            beløp = behandling.totaltFeilutbetaltBeløp().toLong(),
            revurderingsvedtaksdato = tilbakekreving.eksternFagsak.behandlinger.nåværende().entry.vedtaksdato,
            fristdatoForTilbakemelding = Constants.brukersSvarfrist(),
            feilutbetaltePerioder = behandling.feilutbetaltePerioder(),
        )
    }
}
