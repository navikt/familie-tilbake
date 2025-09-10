package no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak

import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunnType

data class SærligeGrunner(
    val erSærligeGrunnerTilReduksjon: Boolean = false,
    val særligeGrunner: List<SærligGrunnType> = emptyList(),
)
