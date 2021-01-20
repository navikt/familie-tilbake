package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import java.time.LocalDate
import java.util.*

data class Behandling(@Id
                      val id: UUID = UUID.randomUUID(),
                      val fagsakId: UUID,
                      val status: Behandlingsstatus = Behandlingsstatus.OPPRETTET,
                      val type: Behandlingstype,
                      val saksbehandlingstype: Saksbehandlingstype = Saksbehandlingstype.ORDINÆR,
                      val opprettetDato: LocalDate = LocalDate.now(),
                      val avsluttetDato: LocalDate?,
                      val ansvarligSaksbehandler: String?,
                      val ansvarligBeslutter: String?,
                      val behandlendeEnhet: String?,
                      val behandlendeEnhetsNavn: String?,
                      val manueltOpprettet: Boolean,
                      val eksternId: UUID?,
                      @MappedCollection(idColumn = "behandling_id")
                      val eksternBehandling: Set<EksternBehandling> = setOf(),
                      val versjon: Int = 0,
                      @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                      val sporbar: Sporbar = Sporbar())

enum class Behandlingsstatus(val kode: String) {

    AVSLUTTET("AVSLU"),
    FATTER_VEDTAK("FVED"),
    IVERKSETTER_VEDTAK("IVED"),
    OPPRETTET("OPPRE"),
    UTREDES("UTRED")
}

enum class Behandlingstype(val kode: String) {

    TILBAKEKREVING("BT-007"),
    REVURDERING_TILBAKEKREVING("BT-009"),
    UDEFINERT("-")
}

enum class Saksbehandlingstype {
    ORDINÆR,
    AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP
}