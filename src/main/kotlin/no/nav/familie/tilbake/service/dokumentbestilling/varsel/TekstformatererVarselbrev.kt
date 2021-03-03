package no.nav.familie.tilbake.service.dokumentbestilling.varsel

import com.github.jknack.handlebars.Template
import no.nav.familie.tilbake.behandling.domain.Varsel
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.FellesTekstformaterer
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.dto.Brevoverskriftsdata
import no.nav.familie.tilbake.service.dokumentbestilling.varsel.handlebars.dto.Varselbrevsdokument
import java.io.IOException
import java.time.LocalDate

object TekstformatererVarselbrev {

    fun lagVarselbrevsfritekst(varselbrevSamletInfo: VarselbrevSamletInfo,
                               varsel: Varsel? = null): String {
        return try {
            val template: Template =
                    FellesTekstformaterer.opprettHandlebarsTemplate(if (varsel == null) "varsel/varsel"
                                                                    else "varsel/korrigert_varsel",
                                                                    varselbrevSamletInfo.brevmetadata.språkkode)
            val varselbrevsdokument: Varselbrevsdokument = mapTilVarselbrevsdokument(varselbrevSamletInfo, varsel)
            FellesTekstformaterer.applyTemplate(template, varselbrevsdokument)
        } catch (e: IOException) {
            throw IllegalStateException("Feil ved tekstgenerering", e)
        }
    }

    fun lagVarselbrevsoverskrift(brevmetadata: Brevmetadata): String {
        return lagOverskrift(brevmetadata, "varsel/varsel_overskrift")
    }

    fun lagKorrigertVarselbrevsoverskrift(brevmetadata: Brevmetadata): String {
        return lagOverskrift(brevmetadata, "varsel/korrigert_varsel_overskrift")
    }

    private fun lagOverskrift(brevmetadata: Brevmetadata, filsti: String): String {
        return try {
            val brevoverskriftsdata = Brevoverskriftsdata(brevmetadata)
            val template: Template = FellesTekstformaterer.opprettHandlebarsTemplate(filsti, brevmetadata.språkkode)
            template.apply(brevoverskriftsdata)
        } catch (e: IOException) {
            throw IllegalStateException("Feil ved tekstgenerering", e)
        }
    }

    private fun finnSenesteOgTidligsteDatoer(feilutbetaltPerioder: List<Periode>): Periode? {
        if (feilutbetaltPerioder.size == 1) {
            return Periode(feilutbetaltPerioder.first().fom, feilutbetaltPerioder.first().tom)
        }
        return null
    }

    fun mapTilVarselbrevsdokument(varselbrevSamletInfo: VarselbrevSamletInfo, varselInfo: Varsel? = null): Varselbrevsdokument {
        return Varselbrevsdokument(brevmetadata = varselbrevSamletInfo.brevmetadata,
                                   beløp = varselbrevSamletInfo.sumFeilutbetaling,
                                   endringsdato = varselbrevSamletInfo.revurderingsvedtaksdato ?: LocalDate.now(),
                                   fristdatoForTilbakemelding = varselbrevSamletInfo.fristdato,
                                   varseltekstFraSaksbehandler = varselbrevSamletInfo.fritekstFraSaksbehandler,
                                   feilutbetaltePerioder = varselbrevSamletInfo.feilutbetaltePerioder,
                                   annenMottagersNavn = BrevmottagerUtil.getannenMottagersNavn(varselbrevSamletInfo.brevmetadata),
                                   finnesVerge = varselbrevSamletInfo.brevmetadata.finnesVerge,
                                   datoerHvisSammenhengendePeriode =
                                   finnSenesteOgTidligsteDatoer(varselbrevSamletInfo.feilutbetaltePerioder),
                                   erKorrigert = varselInfo != null,
                                   varsletDato = varselInfo?.sporbar?.opprettetTid?.toLocalDate(),
                                   varsletBeløp = varselInfo?.varselbeløp)
    }
}
