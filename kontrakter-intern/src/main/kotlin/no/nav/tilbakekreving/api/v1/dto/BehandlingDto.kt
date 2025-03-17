package no.nav.tilbakekreving.api.v1.dto

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsresultatstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsstatus
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingstype
import no.nav.tilbakekreving.kontrakter.behandling.Behandlingsårsakstype
import no.nav.tilbakekreving.kontrakter.behandling.Saksbehandlingstype
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak

data class BehandlingDto(
    val eksternBrukId: UUID,
    val behandlingId: UUID,
    val erBehandlingHenlagt: Boolean,
    val type: Behandlingstype,
    val status: Behandlingsstatus,
    val opprettetDato: LocalDate,
    val avsluttetDato: LocalDate? = null,
    val endretTidspunkt: LocalDateTime,
    val vedtaksdato: LocalDate? = null,
    val enhetskode: String,
    val enhetsnavn: String,
    val resultatstype: Behandlingsresultatstype? = null,
    val ansvarligSaksbehandler: String,
    val ansvarligBeslutter: String? = null,
    val erBehandlingPåVent: Boolean,
    val kanHenleggeBehandling: Boolean,
    val kanRevurderingOpprettes: Boolean = false,
    val harVerge: Boolean,
    val kanEndres: Boolean,
    val kanSetteTilbakeTilFakta: Boolean,
    val varselSendt: Boolean,
    val behandlingsstegsinfo: List<BehandlingsstegsinfoDto>,
    val fagsystemsbehandlingId: String,
    val eksternFagsakId: String,
    val behandlingsårsakstype: Behandlingsårsakstype? = null,
    val støtterManuelleBrevmottakere: Boolean,
    val harManuelleBrevmottakere: Boolean,
    val manuelleBrevmottakere: List<ManuellBrevmottakerResponsDto>,
    val begrunnelseForTilbakekreving: String?,
    val saksbehandlingstype: Saksbehandlingstype,
)

data class BehandlingsstegsinfoDto(
    val behandlingssteg: Behandlingssteg,
    val behandlingsstegstatus: Behandlingsstegstatus,
    val venteårsak: Venteårsak? = null,
    val tidsfrist: LocalDate? = null,
)
