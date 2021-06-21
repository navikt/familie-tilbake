package no.nav.familie.tilbake.datavarehus.saksstatistikk.vedtak

import no.nav.familie.kontrakter.felles.tilbakekreving.Ytelsestype
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import java.time.OffsetDateTime
import java.util.UUID
import javax.validation.constraints.Size

class Vedtaksoppsummering(@Size(min = 1, max = 20)
                          val saksnummer: String,
                          val ytelsestype: Ytelsestype,
                          val behandlingUuid: UUID,
                          val forrigeBehandling: UUID? = null,
                          val referertFagsaksbehandling: String,
                          val behandlingstype: Behandlingstype,
                          val erBehandlingManueltOpprettet: Boolean = false,
                          val behandlendeEnhetsKode: String,
                          val ansvarligSaksbehandler: String,
                          val ansvarligBeslutter: String,
                          val behandlingOpprettetTid: OffsetDateTime,
                          val vedtakFattetTid: OffsetDateTime,
                          @Size(min = 1, max = 100)
                          val perioder: List<VedtakPeriode>)