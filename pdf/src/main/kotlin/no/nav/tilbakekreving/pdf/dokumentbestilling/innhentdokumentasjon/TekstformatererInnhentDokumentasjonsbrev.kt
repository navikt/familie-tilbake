package no.nav.tilbakekreving.pdf.dokumentbestilling.innhentdokumentasjon

import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata
import no.nav.tilbakekreving.pdf.dokumentbestilling.innhentdokumentasjon.handlebars.dto.InnhentDokumentasjonsbrevsdokument
import no.nav.tilbakekreving.pdf.handlebars.FellesTekstformaterer

object TekstformatererInnhentDokumentasjonsbrev {
    fun lagFritekst(dokument: InnhentDokumentasjonsbrevsdokument): String = FellesTekstformaterer.lagBrevtekst(dokument, "innhentdokumentasjon/innhent_dokumentasjon")

    fun lagOverskrift(brevmetadata: Brevmetadata): String = FellesTekstformaterer.lagBrevtekst(brevmetadata, "innhentdokumentasjon/innhent_dokumentasjon_overskrift")
}
