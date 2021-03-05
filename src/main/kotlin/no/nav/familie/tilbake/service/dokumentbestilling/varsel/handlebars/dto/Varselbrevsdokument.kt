package no.nav.familie.tilbake.service.dokumentbestilling.varsel.handlebars.dto

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.common.Periode
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.dto.BaseDokument
import java.time.LocalDate

data class Varselbrevsdokument(val brevmetadata: Brevmetadata,
                               val beløp: Long,
                               val endringsdato: LocalDate,
                               val feilutbetaltePerioder: List<Periode>,
                               val varseltekstFraSaksbehandler: String? = null,
                               val fristdatoForTilbakemelding: LocalDate,
                               val datoerHvisSammenhengendePeriode: Periode? = null,
                               val varsletDato: LocalDate? = null,
                               val varsletBeløp: Long? = null,
                               val finnesVerge: Boolean = false,
                               val annenMottagersNavn: String? = null,
                               val erKorrigert: Boolean = false) : BaseDokument(brevmetadata.ytelsestype,
                                                                                brevmetadata.språkkode,
                                                                                brevmetadata.behandlendeEnhetsNavn,
                                                                                brevmetadata.ansvarligSaksbehandler) {


    @Suppress("unused") // Handlebars
    val isYtelseMedSkatt
        get() = ytelsestype == Ytelsestype.OVERGANGSSTØNAD

    @Suppress("unused") // Handlebars
    val isRentepliktig
        get() = ytelsestype != Ytelsestype.BARNETRYGD

    @Suppress("unused") // Handlebars
    val isFinnesVerge
        get() = finnesVerge

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
            requireNotNull(annenMottagersNavn) { "annenMottagersNavn" }
        }
    }

}
