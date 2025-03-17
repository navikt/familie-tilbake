package no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode

import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.HendelsesundertypePerHendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsestype
import no.nav.tilbakekreving.kontrakter.faktaomfeilutbetaling.Hendelsesundertype

data class HbFakta(
    val hendelsestype: Hendelsestype,
    val hendelsesundertype: Hendelsesundertype,
    val fritekstFakta: String? = null,
) {
    init {
        require(HendelsesundertypePerHendelsestype.getHendelsesundertyper(hendelsestype).contains(hendelsesundertype))
    }
}
