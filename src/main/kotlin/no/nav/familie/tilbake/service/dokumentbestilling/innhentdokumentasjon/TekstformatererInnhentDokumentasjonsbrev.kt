package no.nav.familie.tilbake.service.dokumentbestilling.innhentdokumentasjon

import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.FellesTekstformaterer
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.dto.Brevoverskriftsdata
import no.nav.familie.tilbake.service.dokumentbestilling.innhentdokumentasjon.handlebars.dto.InnhentDokumentasjonsbrevsdokument
import java.io.IOException

internal object TekstformatererInnhentDokumentasjonsbrev {

    fun lagInnhentDokumentasjonsbrevsfritekst(innhentDokumentasjonBrevSamletInfo: InnhentDokumentasjonsbrevSamletInfo): String {
        return try {
            val template =
                    FellesTekstformaterer.opprettHandlebarsTemplate("innhentdokumentasjon/innhent_dokumentasjon",
                                                                    innhentDokumentasjonBrevSamletInfo.brevmetadata.språkkode)
            val innhentDokumentasjonBrevDokument: InnhentDokumentasjonsbrevsdokument = mapTilInnhentDokumentasjonsbrevsdokument(
                    innhentDokumentasjonBrevSamletInfo)
            FellesTekstformaterer.applyTemplate(template, innhentDokumentasjonBrevDokument)
        } catch (e: IOException) {
            throw IllegalStateException("Feil ved tekstgenerering", e)
        }
    }

    fun lagInnhentDokumentasjonsbrevsoverskrift(innhentDokumentasjonBrevSamletInfo: InnhentDokumentasjonsbrevSamletInfo): String {
        return try {
            val template =
                    FellesTekstformaterer.opprettHandlebarsTemplate("innhentdokumentasjon/innhent_dokumentasjon_overskrift",
                                                                    innhentDokumentasjonBrevSamletInfo.brevmetadata.språkkode)
            val overskriftBrevData = Brevoverskriftsdata(innhentDokumentasjonBrevSamletInfo.brevmetadata)
            template.apply(overskriftBrevData)
        } catch (e: IOException) {
            throw IllegalStateException("Feil ved tekstgenerering", e)
        }
    }

    private fun mapTilInnhentDokumentasjonsbrevsdokument(info: InnhentDokumentasjonsbrevSamletInfo)
            : InnhentDokumentasjonsbrevsdokument {
        return InnhentDokumentasjonsbrevsdokument(brevmetadata = info.brevmetadata,
                                                  fritekstFraSaksbehandler = info.fritekstFraSaksbehandler,
                                                  fristdato = info.fristdato,
                                                  finnesVerge = info.brevmetadata.finnesVerge,
                                                  annenMottagersNavn = BrevmottagerUtil.getannenMottagersNavn(info.brevmetadata))
                .apply { valider() }
    }
}
