package no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak

import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnType

class SærligeGrunner(
    var erSærligeGrunnerTilReduksjon: Boolean = false,
    var særligeGrunner: List<SærligGrunnType> = emptyList(),
)
