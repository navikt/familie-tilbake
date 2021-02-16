package no.nav.familie.tilbake.api.dto

import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class BehandlingDto(val eksternBrukId: UUID,
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
                         val harVerge: Boolean)
