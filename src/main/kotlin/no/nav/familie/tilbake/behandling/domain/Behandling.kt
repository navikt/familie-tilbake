package no.nav.familie.tilbake.behandling.domain

import no.nav.familie.tilbake.common.repository.Sporbar
import no.nav.familie.tilbake.varsel.Varsel
import no.nav.familie.tilbake.verge.Verge
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import java.time.LocalDate
import java.util.UUID
import javax.persistence.Version

data class Behandling(@Id
                      val id: UUID = UUID.randomUUID(),
                      val fagsakId: UUID,
                      val status: Behandlingsstatus = Behandlingsstatus.OPPRETTET,
                      val type: Behandlingstype,
                      val saksbehandlingstype: Saksbehandlingstype = Saksbehandlingstype.ORDINÆR,
                      val opprettetDato: LocalDate = LocalDate.now(),
                      val avsluttetDato: LocalDate? = null,
                      val ansvarligSaksbehandler: String?,
                      val ansvarligBeslutter: String? = null,
                      val behandlendeEnhet: String?,
                      val behandlendeEnhetsNavn: String?,
                      val manueltOpprettet: Boolean,
                      val eksternBrukId: UUID = UUID.randomUUID(),
                      @MappedCollection(idColumn = "behandling_id")
                      val eksternBehandling: Set<EksternBehandling> = setOf(),
                      @MappedCollection(idColumn = "behandling_id")
                      val varsler: Set<Varsel> = setOf(),
                      @MappedCollection(idColumn = "behandling_id")
                      val verger: Set<Verge> = setOf(),
                      @MappedCollection(idColumn = "behandling_id")
                      val resultater: Set<Behandlingsresultat> = setOf(),
                      @Version
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

    TILBAKEKREVING("TILBAKEKREVING"),
    REVURDERING_TILBAKEKREVING("Tilbakekreving revurdering")
}

enum class Saksbehandlingstype {
    ORDINÆR,
    AUTOMATISK_IKKE_INNKREVING_LAVT_BELØP
}
