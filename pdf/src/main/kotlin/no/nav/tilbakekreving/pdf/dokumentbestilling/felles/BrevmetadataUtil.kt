package no.nav.tilbakekreving.pdf.dokumentbestilling.felles

fun getAnnenMottagersNavn(brevmetadata: Brevmetadata): String? {
    if (brevmetadata.annenMottakersNavn != null) {
        return brevmetadata.annenMottakersNavn
    }

    val mottagernavn: String = brevmetadata.mottageradresse.mottagernavn
    val brukernavn = brevmetadata.sakspartsnavn
    val vergenavn = brevmetadata.vergenavn

    return if (mottagernavn.equals(brukernavn, ignoreCase = true)) {
        if (brevmetadata.finnesVerge) vergenavn else ""
    } else {
        brukernavn
    }
}
