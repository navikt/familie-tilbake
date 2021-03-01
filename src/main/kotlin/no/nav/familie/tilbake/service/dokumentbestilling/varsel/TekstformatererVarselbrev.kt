package no.nav.familie.tilbake.service.dokumentbestilling.varsel

import com.github.jknack.handlebars.Template
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevMetadata
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevMottakerUtil
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.FellesTekstformaterer
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.dto.OverskriftBrevData
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.dto.periode.HbPeriode
import no.nav.familie.tilbake.service.dokumentbestilling.varsel.handlebars.dto.VarselbrevDokument
import java.io.IOException
import java.time.LocalDate

object TekstformatererVarselbrev {

    fun lagVarselbrevFritekst(varselbrevSamletInfo: VarselbrevSamletInfo,
                              varsel: Varsel? = null): String {
        return try {
            val template: Template =
                    FellesTekstformaterer.opprettHandlebarsTemplate(if (varsel == null) "varsel/varsel" else "varsel/korrigert_varsel",
                                                                    varselbrevSamletInfo.brevMetadata.språkkode)
            val varselbrevDokument: VarselbrevDokument = mapTilVarselbrevDokument(varselbrevSamletInfo, varsel)
            FellesTekstformaterer.applyTemplate(template, varselbrevDokument)
        } catch (e: IOException) {
            throw IllegalStateException("Feil ved tekstgenerering", e)
        }
    }

    fun lagVarselbrevOverskrift(brevMetadata: BrevMetadata): String {
        return lagOverskrift(brevMetadata, "varsel/varsel_overskrift")
    }

    fun lagKorrigertVarselbrevOverskrift(brevMetadata: BrevMetadata): String {
        return lagOverskrift(brevMetadata, "varsel/korrigert_varsel_overskrift")
    }

    private fun lagOverskrift(brevMetadata: BrevMetadata, filsti: String): String {
        return try {
            val overskriftBrevData = OverskriftBrevData(brevMetadata)
            val template: Template = FellesTekstformaterer.opprettHandlebarsTemplate(filsti, brevMetadata.språkkode)
            template.apply(overskriftBrevData)
        } catch (e: IOException) {
            throw IllegalStateException("Feil ved tekstgenerering", e)
        }
    }

    private fun finnSenesteOgTidligsteDatoer(feilutbetaltPerioder: List<HbPeriode?>?): HbPeriode? {
        if (feilutbetaltPerioder != null && feilutbetaltPerioder.size == 1) {
            return HbPeriode(feilutbetaltPerioder.first()!!.fom, feilutbetaltPerioder.first()!!.tom)
        }
        return null
    }

    fun mapTilVarselbrevDokument(varselbrevSamletInfo: VarselbrevSamletInfo, varselInfo: Varsel? = null): VarselbrevDokument {
        return VarselbrevDokument(brevMetadata = varselbrevSamletInfo.brevMetadata,
                                  beløp = varselbrevSamletInfo.sumFeilutbetaling,
                                  endringsdato = varselbrevSamletInfo.revurderingVedtakDato ?: LocalDate.now(),
                                  fristdatoForTilbakemelding = varselbrevSamletInfo.fristdato,
                                  varseltekstFraSaksbehandler = varselbrevSamletInfo.fritekstFraSaksbehandler,
                                  feilutbetaltePerioder = varselbrevSamletInfo.feilutbetaltePerioder,
                                  annenMottakerNavn = BrevMottakerUtil.getAnnenMottakerNavn(varselbrevSamletInfo.brevMetadata),
                                  finnesVerge = varselbrevSamletInfo.brevMetadata.finnesVerge,
                                  datoerHvisSammenhengendePeriode =
                                  finnSenesteOgTidligsteDatoer(varselbrevSamletInfo.feilutbetaltePerioder),
                                  erKorrigert = varselInfo != null,
                                  varsletDato = varselInfo?.sporbar?.opprettetTid?.toLocalDate(),
                                  varsletBeløp = varselInfo?.varselbeløp)
    }

}
