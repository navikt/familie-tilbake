package no.nav.familie.tilbake.dokumentbestilling.varsel

import no.nav.familie.tilbake.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.dokumentbestilling.handlebars.FellesTekstformaterer
import no.nav.familie.tilbake.dokumentbestilling.handlebars.dto.Brevoverskriftsdata
import no.nav.familie.tilbake.dokumentbestilling.varsel.handlebars.dto.Varselbrevsdokument

object TekstformatererVarselbrev {

    fun lagFritekst(varselbrevsdokument: Varselbrevsdokument, erKorrigert: Boolean): String {
        val filsti = if (erKorrigert) "varsel/korrigert_varsel" else "varsel/varsel"
        return FellesTekstformaterer.lagBrevtekst(varselbrevsdokument, filsti)
    }

    fun lagVarselbrevsoverskrift(brevmetadata: Brevmetadata, erKorrigert: Boolean): String {
        val filsti = if (erKorrigert) "varsel/korrigert_varsel_overskrift" else "varsel/varsel_overskrift"
        return FellesTekstformaterer.lagBrevtekst(Brevoverskriftsdata(brevmetadata), filsti)
    }
}
