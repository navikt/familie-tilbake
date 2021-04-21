package no.nav.familie.tilbake.service.dokumentbestilling.varsel

import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.FellesTekstformaterer
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.dto.Brevoverskriftsdata
import no.nav.familie.tilbake.service.dokumentbestilling.varsel.handlebars.dto.Varselbrevsdokument

object TekstformatererVarselbrev {

    fun lagFritekst(varselbrevsdokument: Varselbrevsdokument): String {
        return FellesTekstformaterer.lagBrevtekst(varselbrevsdokument, "varsel/varsel")
    }

    fun lagKorrigertFritekst(varselbrevsdokument: Varselbrevsdokument): String {
        return FellesTekstformaterer.lagBrevtekst(varselbrevsdokument, "varsel/korrigert_varsel")
    }

    fun lagVarselbrevsoverskrift(brevmetadata: Brevmetadata): String {
        return FellesTekstformaterer.lagBrevtekst(Brevoverskriftsdata(brevmetadata), "varsel/varsel_overskrift")
    }

    fun lagKorrigertVarselbrevsoverskrift(brevmetadata: Brevmetadata): String {
        return FellesTekstformaterer.lagBrevtekst(Brevoverskriftsdata(brevmetadata), "varsel/korrigert_varsel_overskrift")
    }
}
