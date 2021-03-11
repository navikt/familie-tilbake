package no.nav.familie.tilbake.service.dokumentbestilling.henleggelse.handlebars.dto

import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.service.dokumentbestilling.felles.Brevmetadata
import no.nav.familie.tilbake.service.dokumentbestilling.felles.BrevmottagerUtil
import no.nav.familie.tilbake.service.dokumentbestilling.handlebars.dto.BaseDokument
import java.time.LocalDate
import java.util.Objects

data class Henleggelsesbrevsdokument(val brevmetadata: Brevmetadata,
                                     val varsletDato: LocalDate?,
                                     val fritekstFraSaksbehandler: String?) : BaseDokument(brevmetadata.ytelsestype,
                                                                                           brevmetadata.språkkode,
                                                                                           brevmetadata.behandlendeEnhetsNavn,
                                                                                           brevmetadata.ansvarligSaksbehandler) {

    private val tilbakekrevingsrevurdering = Behandlingstype.REVURDERING_TILBAKEKREVING == brevmetadata.behandlingstype

    val finnesVerge: Boolean = brevmetadata.finnesVerge

    val annenMottagersNavn: String? = BrevmottagerUtil.getannenMottagersNavn(brevmetadata)

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
            requireNotNull(annenMottagersNavn) { "annenMottakerNavn kan ikke være null" }
        }
    }
}