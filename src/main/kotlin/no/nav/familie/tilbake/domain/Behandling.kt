package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.util.*

data class Behandling(@Id
                      val id: UUID = UUID.randomUUID(),
                      val fagsakId: UUID,
                      val behandlingsstatus: String,
                      val behandlingstype: String,
                      val opprettetDato: LocalDate = LocalDate.now(),
                      val avsluttetDato: LocalDate?,
                      val ansvarligSaksbehandler: String?,
                      val ansvarligBeslutter: String?,
                      val behandlendeEnhet: String?,
                      val behandlendeEnhetNavn: String?,
                      val manueltOpprettet: Boolean,
                      val eksternId: UUID?,
                      val saksbehandlingstype: String,
                      val versjon: Int = 0,
                      @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                      val sporbar: Sporbar = Sporbar())