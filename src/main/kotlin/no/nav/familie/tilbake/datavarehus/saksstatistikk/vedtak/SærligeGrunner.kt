package no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak

import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunn

data class SærligeGrunner(
    val erSærligeGrunnerTilReduksjon: Boolean = false,
    val særligeGrunner: List<SærligGrunn> = emptyList(),
)
