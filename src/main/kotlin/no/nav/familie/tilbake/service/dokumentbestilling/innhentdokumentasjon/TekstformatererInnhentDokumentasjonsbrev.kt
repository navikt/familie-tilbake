package no.nav.familie.tilbake.service.dokumentbestilling.innhentdokumentasjon

import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.FellesTekstformaterer
import no.nav.familie.tilbake.service.dokumentbestilling.innhentdokumentasjon.handlebars.dto.InnhentDokumentasjonsbrevsdokument

internal object TekstformatererInnhentDokumentasjonsbrev {

    fun lagFritekst(dokument: InnhentDokumentasjonsbrevsdokument): String {
        return FellesTekstformaterer.lagBrevtekst(dokument, "innhentdokumentasjon/innhent_dokumentasjon")
    }

    fun lagOverskrift(brevmetadata: Brevmetadata): String {
        return FellesTekstformaterer.lagBrevtekst(brevmetadata, "innhentdokumentasjon/innhent_dokumentasjon_overskrift")
    }
}
