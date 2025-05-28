package no.nav.tilbakekreving.pdf.dokumentbestilling.innhentdokumentasjon.handlebars.dto

import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.getAnnenMottagersNavn
import no.nav.tilbakekreving.pdf.handlebars.dto.BaseDokument
import java.time.LocalDate
import java.util.Objects

data class InnhentDokumentasjonsbrevsdokument(
    val brevmetadata: Brevmetadata,
    val fritekstFraSaksbehandler: String,
    val fristdato: LocalDate,
) : BaseDokument(
        brevmetadata.ytelsestype,
        brevmetadata.språkkode,
        brevmetadata.behandlendeEnhetsNavn,
        brevmetadata.ansvarligSaksbehandler,
        brevmetadata.gjelderDødsfall,
        brevmetadata.institusjon,
    ) {
    val finnesVerge: Boolean = brevmetadata.finnesVerge

    val annenMottagersNavn: String? = getAnnenMottagersNavn(brevmetadata)

    @Suppress("unused") // Handlebars
    val isRentepliktig = ytelsestype != Ytelsestype.BARNETRYGD && ytelsestype != Ytelsestype.KONTANTSTØTTE

    @Suppress("unused") // Handlebars
    val isBarnetrygd = ytelsestype == Ytelsestype.BARNETRYGD

    @Suppress("unused") // Handlebars
    val isKontantstøtte = ytelsestype == Ytelsestype.KONTANTSTØTTE

    init {
        if (finnesVerge) {
            Objects.requireNonNull(annenMottagersNavn, "annenMottagersNavn kan ikke være null")
        }
    }
}
