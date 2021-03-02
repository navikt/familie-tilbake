package no.nav.familie.tilbake.service.dokumentbestilling.innhentdokumentasjon.handlebars.dto

import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.dto.BaseDokument
import java.time.LocalDate
import java.util.Objects

class InnhentDokumentasjonsbrevsdokument(val brevmetadata: Brevmetadata,
                                         val fritekstFraSaksbehandler: String,
                                         val fristdato: LocalDate,
                                         val finnesVerge: Boolean,
                                         val annenMottagersNavn: String? = null)
    : BaseDokument(brevmetadata.ytelsestype,
                   brevmetadata.språkkode,
                   brevmetadata.behandlendeEnhetsNavn,
                   brevmetadata.ansvarligSaksbehandler) {

    fun valider() {
        if (finnesVerge) {
            Objects.requireNonNull(annenMottagersNavn, "annenMottagersNavn kan ikke være null")
        }
    }
}
