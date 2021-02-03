package no.nav.familie.tilbake.api.dto

import no.nav.familie.tilbake.behandling.domain.Behandlingsresultatstype
import no.nav.familie.tilbake.behandling.domain.Behandlingsstatus
import no.nav.familie.tilbake.behandling.domain.Behandlingstype
import no.nav.familie.tilbake.behandling.domain.Fagsaksstatus
import no.nav.familie.tilbake.behandling.domain.Ytelsestype
import no.nav.familie.tilbake.integration.pdl.internal.Kjønn
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class HentBehandlingResponsDto(val behandling: BehandlingDto,
                                    val fagsak: FagsakDto,
                                    val bruker: BrukerDto)

data class BehandlingDto(val eksternBrukId: UUID,
                         val erBehandlingHenlagt: Boolean,
                         val type: Behandlingstype,
                         val status: Behandlingsstatus,
                         val språkkode: String,
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


data class FagsakDto(val eksternFagsakId: String,
                     val status: Fagsaksstatus,
                     val ytelsestype: Ytelsestype,
                     val søkerFødselsnummer: String)

data class BrukerDto(val navn: String,
                     val fødselsdato: LocalDate,
                     val kjønn: Kjønn)
