package no.nav.tilbakekreving.breeeev

import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler
import no.nav.tilbakekreving.breeeev.begrunnelse.VilkårsvurderingBegrunnelse
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode

data class BegrunnetPeriode(
    val periode: Datoperiode,
    val meldingerTilSaksbehandler: Set<MeldingTilSaksbehandler>,
    val påkrevdeVurderinger: Set<VilkårsvurderingBegrunnelse>,
)
