package no.nav.tilbakekreving.vedtak

import no.nav.tilbakekreving.entities.AktørEntity
import no.nav.tilbakekreving.fagsystem.Ytelsestype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsperiodeDto
import java.math.BigInteger
import java.time.LocalDate
import java.util.UUID

data class IverksattVedtak(
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val vedtakId: BigInteger,
    val aktør: AktørEntity,
    val opprettetTid: LocalDate,
    val ytelsestype: Ytelsestype,
    val kvittering: String,
    val tilbakekrevingsperioder: List<TilbakekrevingsperiodeDto>,
    val behandlingstype: Behandlingstype,
)
