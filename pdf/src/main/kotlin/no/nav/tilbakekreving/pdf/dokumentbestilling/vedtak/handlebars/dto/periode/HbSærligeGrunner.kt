package no.nav.tilbakekreving.pdf.dokumentbestilling.vedtak.handlebars.dto.periode

import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnTyper

data class HbSærligeGrunner(
    val størrelse: Boolean = false,
    val annet: Boolean = false,
    val navfeil: Boolean = false,
    val tid: Boolean = false,
    val fritekst: String? = null,
    val fritekstAnnet: String? = null,
) {
    constructor(
        grunner: Collection<SærligGrunnTyper>,
        fritekst: String? = null,
        fritekstAnnet: String? = null,
    ) : this(
        grunner.contains(SærligGrunnTyper.STØRRELSE_BELØP),
        grunner.contains(SærligGrunnTyper.ANNET),
        grunner.contains(SærligGrunnTyper.HELT_ELLER_DELVIS_NAVS_FEIL),
        grunner.contains(SærligGrunnTyper.TID_FRA_UTBETALING),
        fritekst,
        fritekstAnnet,
    )
}
