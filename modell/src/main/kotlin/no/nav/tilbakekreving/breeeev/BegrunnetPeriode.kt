package no.nav.tilbakekreving.breeeev

import no.nav.tilbakekreving.breeeev.begrunnelse.MeldingTilSaksbehandler
import no.nav.tilbakekreving.breeeev.begrunnelse.VilkårsvurderingBegrunnelse
import no.nav.tilbakekreving.kontrakter.periode.Datoperiode
import java.util.UUID

data class BegrunnetPeriode(
    val id: UUID,
    val periode: Datoperiode,
    val meldingerTilSaksbehandler: Set<MeldingTilSaksbehandler>,
    val påkrevdeVurderinger: Set<VilkårsvurderingBegrunnelse>,
)
