package no.nav.tilbakekreving.behov

import no.nav.tilbakekreving.aktør.Aktør
import no.nav.tilbakekreving.beregning.delperiode.Delperiode
import no.nav.tilbakekreving.fagsystem.Ytelse
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import java.util.UUID

class IverksettelseBehov(
    val behandlingId: UUID,
    val kravgrunnlagId: String,
    val delperioder: List<Delperiode<out Delperiode.Beløp>>,
    val ansvarligSaksbehandler: String,
    val ytelse: Ytelse,
    val aktør: Aktør,
    val behandlingstype: Behandlingstype,
) : Behov
