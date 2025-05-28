package no.nav.tilbakekreving.pdf.dokumentbestilling.henleggelse.handlebars.dto

import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.Brevmetadata
import no.nav.tilbakekreving.pdf.dokumentbestilling.felles.getAnnenMottagersNavn
import no.nav.tilbakekreving.pdf.handlebars.dto.BaseDokument
import java.time.LocalDate
import java.util.Objects

data class Henleggelsesbrevsdokument(
    val brevmetadata: Brevmetadata,
    val varsletDato: LocalDate?,
    val fritekstFraSaksbehandler: String?,
) : BaseDokument(
        brevmetadata.ytelsestype,
        brevmetadata.språkkode,
        brevmetadata.behandlendeEnhetsNavn,
        brevmetadata.ansvarligSaksbehandler,
        brevmetadata.gjelderDødsfall,
        brevmetadata.institusjon,
    ) {
    private val tilbakekrevingsrevurdering = Behandlingstype.REVURDERING_TILBAKEKREVING == brevmetadata.behandlingstype

    val finnesVerge: Boolean = brevmetadata.finnesVerge

    val annenMottagersNavn: String? = getAnnenMottagersNavn(brevmetadata)

    init {
        if (finnesVerge) {
            Objects.requireNonNull(annenMottagersNavn, "annenMottagersNavn kan ikke være null")
        }
    }

    fun init() {
        if (tilbakekrevingsrevurdering) {
            requireNotNull(fritekstFraSaksbehandler) { "fritekst kan ikke være null" }
        } else {
            requireNotNull(varsletDato) { "varsletDato kan ikke være null" }
        }
        if (finnesVerge) {
            requireNotNull(annenMottagersNavn) { "annenMottagersNavn kan ikke være null" }
        }
    }
}
