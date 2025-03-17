package no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak

import no.nav.tilbakekreving.kontrakter.vilkårsvurdering.SærligGrunn

class SærligeGrunner(
    var erSærligeGrunnerTilReduksjon: Boolean = false,
    var særligeGrunner: List<SærligGrunn> = emptyList(),
)
