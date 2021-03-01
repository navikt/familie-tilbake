package no.nav.familie.tilbake.service.dokumentbestilling.innhentdokumentasjon

import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevMottakerUtil
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.FellesTekstformaterer
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.dto.OverskriftBrevData
import no.nav.familie.tilbake.service.dokumentbestilling.innhentdokumentasjon.handlebars.dto.InnhentDokumentasjonbrevDokument
import java.io.IOException

internal object TekstformatererInnhentDokumentasjonbrev {

    fun lagInnhentDokumentasjonBrevFritekst(innhentDokumentasjonBrevSamletInfo: InnhentDokumentasjonsbrevSamletInfo): String {
        return try {
            val template = FellesTekstformaterer.opprettHandlebarsTemplate("innhentdokumentasjon/innhent_dokumentasjon",
                                                                           innhentDokumentasjonBrevSamletInfo.brevMetadata.språkkode)
            val innhentDokumentasjonBrevDokument: InnhentDokumentasjonbrevDokument = mapTilInnhentDokumentasjonBrevDokument(
                    innhentDokumentasjonBrevSamletInfo)
            FellesTekstformaterer.applyTemplate(template, innhentDokumentasjonBrevDokument)
        } catch (e: IOException) {
            throw IllegalStateException("Feil ved tekstgenerering", e)
        }
    }

    fun lagInnhentDokumentasjonBrevOverskrift(innhentDokumentasjonBrevSamletInfo: InnhentDokumentasjonsbrevSamletInfo): String {
        return try {
            val template =
                    FellesTekstformaterer.opprettHandlebarsTemplate("innhentdokumentasjon/innhent_dokumentasjon_overskrift",
                                                                    innhentDokumentasjonBrevSamletInfo.brevMetadata.språkkode)
            val overskriftBrevData = OverskriftBrevData(innhentDokumentasjonBrevSamletInfo.brevMetadata)
            template.apply(overskriftBrevData)
        } catch (e: IOException) {
            throw IllegalStateException("Feil ved tekstgenerering", e)
        }
    }

    private fun mapTilInnhentDokumentasjonBrevDokument(info: InnhentDokumentasjonsbrevSamletInfo)
            : InnhentDokumentasjonbrevDokument {
        return InnhentDokumentasjonbrevDokument(brevMetadata = info.brevMetadata,
                                                fritekstFraSaksbehandler = info.fritekstFraSaksbehandler,
                                                fristDato = info.fristDato,
                                                finnesVerge = info.brevMetadata.finnesVerge,
                                                annenMottakerNavn = BrevMottakerUtil.getAnnenMottakerNavn(info.brevMetadata))
                .apply { valider() }
    }
}
