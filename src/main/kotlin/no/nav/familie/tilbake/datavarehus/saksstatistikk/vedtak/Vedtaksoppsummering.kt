package no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak

import jakarta.validation.constraints.Size
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.ytelse.Fagsystem
import no.nav.tilbakekreving.kontrakter.ytelse.Ytelsestype
import java.time.OffsetDateTime
import java.util.UUID

class Vedtaksoppsummering(
    @Size(min = 1, max = 20)
    val saksnummer: String,
    val ytelsestype: Ytelsestype,
    val fagsystem: Fagsystem,
    val behandlingUuid: UUID,
    val behandlingstype: Behandlingstype,
    val erBehandlingManueltOpprettet: Boolean = false,
    val behandlingOpprettetTidspunkt: OffsetDateTime,
    val vedtakFattetTidspunkt: OffsetDateTime,
    val referertFagsaksbehandling: String,
    val forrigeBehandling: UUID? = null,
    val ansvarligSaksbehandler: String,
    val ansvarligBeslutter: String,
    val behandlendeEnhet: String,
    @Size(min = 1, max = 100)
    val perioder: List<VedtakPeriode>,
)
