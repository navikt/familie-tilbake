package no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode

import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype
import no.nav.tilbakekreving.pdf.HendelsesundertypePerHendelsestype

data class HbFakta(
    val hendelsestype: Hendelsestype,
    val hendelsesundertype: Hendelsesundertype,
    val fritekstFakta: String? = null,
) {
    init {
        require(HendelsesundertypePerHendelsestype.getHendelsesundertyper(hendelsestype).contains(hendelsesundertype))
    }
}
