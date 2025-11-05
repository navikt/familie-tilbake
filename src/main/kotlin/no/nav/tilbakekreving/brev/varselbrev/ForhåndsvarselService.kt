package no.nav.tilbakekreving.brev.varselbrev

import no.nav.familie.tilbake.config.Constants
import no.nav.tilbakekreving.Tilbakekreving
import no.nav.tilbakekreving.brev.VarselbrevInfo
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Adresseinfo
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata
import no.nav.tilbakekreving.pdf.dokumentbestilling.varsel.TekstformatererVarselbrev
import no.nav.tilbakekreving.pdf.dokumentbestilling.varsel.handlebars.dto.Varselbrevsdokument
import org.springframework.stereotype.Service

@Service
class ForhåndsvarselService() {
    fun hentVarselbrevTekster(tilbakekreving: Tilbakekreving): List<Section> {
        val brevmetadata = opprettMetadata(tilbakekreving.hentVarselbrevInfo())
        val overskrift = TekstformatererVarselbrev.lagVarselbrevsoverskrift(brevmetadata, false)
        val brevbody = TekstformatererVarselbrev.lagFritekst(opprettVarselbrevsdokument(tilbakekreving.hentVarselbrevInfo(), brevmetadata), false)

        val varselbrevAvsnitter = VarselbrevParser.parse(brevbody)
        varselbrevAvsnitter.add(0, Section("overskrift", overskrift))
        return varselbrevAvsnitter
    }

    private fun opprettMetadata(varselbrevInfo: VarselbrevInfo): Brevmetadata {
        return Brevmetadata(
            sakspartId = varselbrevInfo.ident,
            sakspartsnavn = varselbrevInfo.navn,
            mottageradresse = Adresseinfo(varselbrevInfo.ident, varselbrevInfo.navn),
            behandlendeEnhetsNavn = varselbrevInfo.behandlendeEnhetsNavn,
            ansvarligSaksbehandler = varselbrevInfo.ansvarligSaksbehandler,
            saksnummer = varselbrevInfo.eksternFagsakId,
            språkkode = varselbrevInfo.språkkode,
            ytelsestype = varselbrevInfo.ytelseType,
            gjelderDødsfall = varselbrevInfo.gjelderDødsfall,
        )
    }

    private fun opprettVarselbrevsdokument(varselbrevInfo: VarselbrevInfo, brevmetadata: Brevmetadata): Varselbrevsdokument {
        return Varselbrevsdokument(
            brevmetadata = brevmetadata,
            beløp = varselbrevInfo.beløp,
            revurderingsvedtaksdato = varselbrevInfo.revurderingsvedtaksdato,
            fristdatoForTilbakemelding = Constants.brukersSvarfrist(),
            feilutbetaltePerioder = varselbrevInfo.feilutbetaltePerioder,
        )
    }
}
