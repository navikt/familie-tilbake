package no.nav.familie.tilbake.service.dokumentbestilling.innhentdokumentasjon.handlebars.dto

import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.dto.BaseDokument
import java.time.LocalDate
import java.util.Objects

data class InnhentDokumentasjonsbrevsdokument(val brevmetadata: Brevmetadata,
                                              val fritekstFraSaksbehandler: String,
                                              val fristdato: LocalDate) : BaseDokument(brevmetadata.ytelsestype,
                                                                                       brevmetadata.språkkode,
                                                                                       brevmetadata.behandlendeEnhetsNavn,
                                                                                       brevmetadata.ansvarligSaksbehandler) {

    val finnesVerge: Boolean = brevmetadata.finnesVerge

    val annenMottagersNavn: String? = BrevmottagerUtil.getannenMottagersNavn(brevmetadata)

    init {
        if (finnesVerge) {
            Objects.requireNonNull(annenMottagersNavn, "annenMottagersNavn kan ikke være null")
        }
    }
}
