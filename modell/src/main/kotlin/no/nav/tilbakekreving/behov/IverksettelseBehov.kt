package no.nav.tilbakekreving.behov

import no.nav.tilbakekreving.beregning.delperiode.Delperiode
import java.util.UUID

class IverksettelseBehov(
    val behandlingId: UUID,
    val kravgrunnlagId: String,
    val delperioder: List<Delperiode<out Delperiode.Beløp>>,
    val ansvarligSaksbehandler: String,
) : Behov
