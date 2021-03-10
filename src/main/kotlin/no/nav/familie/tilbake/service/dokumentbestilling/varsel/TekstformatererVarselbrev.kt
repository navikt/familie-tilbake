package no.nav.familie.tilbake.service.dokumentbestilling.varsel

import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.FellesTekstformaterer
import no.nav.familie.tilbake.service.dokumentbestilling.varsel.handlebars.dto.Varselbrevsdokument
import java.time.LocalDate

object TekstformatererVarselbrev {

    fun lagFritekst(varselbrevsdokument: Varselbrevsdokument): String {
        return FellesTekstformaterer.lagFritekst(varselbrevsdokument, "varsel/varsel")
    }

    fun lagKorrigertFritekst(varselbrevsdokument: Varselbrevsdokument): String {
        return FellesTekstformaterer.lagFritekst(varselbrevsdokument, "varsel/korrigert_varsel")
    }

    fun lagVarselbrevsoverskrift(brevmetadata: Brevmetadata): String {
        return FellesTekstformaterer.lagOverskrift(brevmetadata, "varsel/varsel_overskrift")
    }

    fun lagKorrigertVarselbrevsoverskrift(brevmetadata: Brevmetadata): String {
        return FellesTekstformaterer.lagOverskrift(brevmetadata, "varsel/korrigert_varsel_overskrift")
    }
}
