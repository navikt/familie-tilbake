package no.nav.familie.tilbake.datavarehus.saksstatistikk.sakshendelse

import no.nav.familie.tilbake.behandling.Fagsystem
import no.nav.familie.tilbake.behandling.Ytelsestype
import no.nav.tilbakekreving.kontrakter.Periode
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsresultatstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.UUID

data class Behandlingstilstand(
    val funksjoneltTidspunkt: OffsetDateTime,
    val tekniskTidspunkt: OffsetDateTime? = null,
    val saksnummer: String,
    val fagsystem: Fagsystem,
    val ytelsestype: Ytelsestype,
    val behandlingUuid: UUID,
    val referertFagsaksbehandling: String,
    val behandlingstype: Behandlingstype,
    val behandlingsstatus: Behandlingsstatus,
    val behandlingsresultat: Behandlingsresultatstype,
    val behandlingErManueltOpprettet: Boolean,
    val venterPåBruker: Boolean,
    val venterPåØkonomi: Boolean,
    val ansvarligEnhet: String,
    val ansvarligSaksbehandler: String,
    val ansvarligBeslutter: String?,
    val forrigeBehandling: UUID?,
    val revurderingOpprettetÅrsak: Behandlingsårsakstype?,
    val totalFeilutbetaltBeløp: BigDecimal?,
    val totalFeilutbetaltPeriode: Periode?,
)
