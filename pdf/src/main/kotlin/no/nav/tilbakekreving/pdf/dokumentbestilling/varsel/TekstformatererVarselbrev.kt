package no.nav.tilbakekreving.pdf.dokumentbestilling.varsel

import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata
import no.nav.tilbakekreving.pdf.dokumentbestilling.varsel.handlebars.dto.Varselbrevsdokument
import no.nav.tilbakekreving.pdf.dokumentbestilling.varsel.handlebars.dto.Vedleggsdata
import no.nav.tilbakekreving.pdf.handlebars.FellesTekstformaterer
import no.nav.tilbakekreving.pdf.handlebars.dto.Brevoverskriftsdata

object TekstformatererVarselbrev {
    fun lagFritekst(
        varselbrevsdokument: Varselbrevsdokument,
        erKorrigert: Boolean,
    ): String {
        val filsti = if (erKorrigert) "varsel/korrigert_varsel" else "varsel/varsel"
        return FellesTekstformaterer.lagBrevtekst(varselbrevsdokument, filsti)
    }

    fun lagVarselbrevsoverskrift(
        brevmetadata: Brevmetadata,
        erKorrigert: Boolean,
    ): String {
        val filsti = if (erKorrigert) "varsel/korrigert_varsel_overskrift" else "varsel/varsel_overskrift"
        return FellesTekstformaterer.lagBrevtekst(Brevoverskriftsdata(brevmetadata), filsti)
    }

    fun lagVarselbrevsvedleggHtml(vedleggsdata: Vedleggsdata): String = FellesTekstformaterer.lagBrevtekst(vedleggsdata, "varsel/vedlegg")
}
