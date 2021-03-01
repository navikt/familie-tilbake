package no.nav.familie.tilbake.service.dokumentbestilling.innhentdokumentasjon.handlebars.dto

import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevMetadata
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.dto.BaseDokument
import java.time.LocalDate
import java.util.Objects

class InnhentDokumentasjonbrevDokument(val brevMetadata: BrevMetadata,
                                       val fritekstFraSaksbehandler: String,
                                       val fristDato: LocalDate,
                                       val finnesVerge: Boolean,
                                       val annenMottakerNavn: String? = null)
    : BaseDokument(brevMetadata.ytelsestype,
                   brevMetadata.språkkode,
                   brevMetadata.behandlendeEnhetNavn,
                   brevMetadata.ansvarligSaksbehandler) {

    fun valider() {
        if (finnesVerge) {
            Objects.requireNonNull(annenMottakerNavn, "annenMottakerNavn kan ikke være null")
        }
    }
}
