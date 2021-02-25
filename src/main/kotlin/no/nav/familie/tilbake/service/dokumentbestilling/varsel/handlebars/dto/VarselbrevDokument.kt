package no.nav.familie.tilbake.service.dokumentbestilling.varsel.handlebars.dto

import no.nav.familie.kontrakter.felles.tilbakekreving.Språkkode
import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevMetadata
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.dto.BaseDokument
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.dto.periode.HbPeriode
import java.time.LocalDate

data class VarselbrevDokument(val brevMetadata: BrevMetadata,
                              val beløp: Long,
                              val endringsdato: LocalDate,
                              val feilutbetaltePerioder: List<HbPeriode>,
                              val varseltekstFraSaksbehandler: String? = null,
                              val fristdatoForTilbakemelding: LocalDate,
                              val datoerHvisSammenhengendePeriode: HbPeriode? = null,
                              val varsletDato: LocalDate? = null,
                              val varsletBeløp: Long? = null,
                              val finnesVerge: Boolean = false,
                              val annenMottakerNavn: String? = null,
                              val erKorrigert: Boolean = false) : BaseDokument(brevMetadata.ytelsestype,
                                                                               brevMetadata.språkkode,
                                                                               brevMetadata.behandlendeEnhetNavn,
                                                                               brevMetadata.ansvarligSaksbehandler) {


    @Suppress("Brukes av handlebars")
    val isYtelseMedSkatt
        get() = ytelsestype == Ytelsestype.OVERGANGSSTØNAD

    @Suppress("Brukes av handlebars")
    val isRentepliktig
        get() = ytelsestype != Ytelsestype.BARNETRYGD

    init {
        if (feilutbetaltePerioder.size == 1) {
            requireNotNull(datoerHvisSammenhengendePeriode) { "datoer for sammenhengende periode" }
        } else if (feilutbetaltePerioder.size > 1) {
            feilutbetaltePerioder.forEach {
                requireNotNull(it.fom) { "fraogmed-dato for feilutbetalingsperiode" }
                requireNotNull(it.tom) { "tilogmed-dato for feilutbetalingsperiode" }
            }
        }
        if (erKorrigert) {
            requireNotNull(varsletDato) { "varsletDato" }
            requireNotNull(varsletBeløp) { "varsletBelop" }
        }
        if (finnesVerge) {
            requireNotNull(annenMottakerNavn) { "annenMottakerNavn" }
        }
    }

}