package no.nav.familie.tilbake.dokumentbestilling.vedtak.handlebars.dto.periode

import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsestype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.Hendelsesundertype
import no.nav.familie.tilbake.faktaomfeilutbetaling.domain.HendelsesundertypePerHendelsestype

data class HbFakta(
    val hendelsestype: Hendelsestype,
    val hendelsesundertype: Hendelsesundertype,
    val fritekstFakta: String? = null,
    val grunnbeløpsperioder: List<HbGrunnbeløpsperiode> = emptyList()
) {

    init {
        require(HendelsesundertypePerHendelsestype.getHendelsesundertyper(hendelsestype).contains(hendelsesundertype))
        validerInntektOver6G()
    }

    private fun validerInntektOver6G() {
        if (hendelsesundertype == Hendelsesundertype.INNTEKT_OVER_6G) {
            require(grunnbeløpsperioder.isNotEmpty()) { "Grunnbeløpsperioder er påkrevd hendelsesundertype=$hendelsesundertype" }
        }
    }
}
