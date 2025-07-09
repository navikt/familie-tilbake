package no.nav.tilbakekreving.vedtak

import no.nav.familie.tilbake.common.repository.Sporbar
import no.nav.tilbakekreving.entities.AktørEntity
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.tilbakekrevingsvedtak.vedtak.v1.TilbakekrevingsvedtakDto
import java.math.BigInteger
import java.util.UUID

data class IverksattVedtak(
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val vedtakId: BigInteger,
    val aktør: AktørEntity,
    val ytelsestypeKode: String,
    val kvittering: String?,
    val tilbakekrevingsvedtak: TilbakekrevingsvedtakDto,
    val sporbar: Sporbar = Sporbar(),
    val behandlingstype: Behandlingstype,
)
