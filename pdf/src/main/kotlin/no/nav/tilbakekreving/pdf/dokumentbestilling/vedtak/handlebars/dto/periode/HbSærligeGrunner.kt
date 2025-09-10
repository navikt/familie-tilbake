package no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode

import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnType

data class HbSærligeGrunner(
    val størrelse: Boolean = false,
    val annet: Boolean = false,
    val navfeil: Boolean = false,
    val tid: Boolean = false,
    val fritekst: String? = null,
    val fritekstAnnet: String? = null,
) {
    constructor(
        grunner: Collection<SærligGrunnType>,
        fritekst: String? = null,
        fritekstAnnet: String? = null,
    ) : this(
        grunner.contains(SærligGrunnType.STØRRELSE_BELØP),
        grunner.contains(SærligGrunnType.ANNET),
        grunner.contains(SærligGrunnType.HELT_ELLER_DELVIS_NAVS_FEIL),
        grunner.contains(SærligGrunnType.TID_FRA_UTBETALING),
        fritekst,
        fritekstAnnet,
    )
}
