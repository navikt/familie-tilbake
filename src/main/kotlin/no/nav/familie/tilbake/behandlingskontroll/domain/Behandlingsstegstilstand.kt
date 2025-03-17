package no.nav.familie.tilbake.behandlingskontroll.domain

import no.nav.familie.tilbake.common.repository.Sporbar
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingssteg
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Behandlingsstegstatus
import no.nav.tilbakekreving.kontrakter.behandlingskontroll.Venteårsak
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Embedded
import java.time.LocalDate
import java.util.UUID

data class Behandlingsstegstilstand(
    @Id
    val id: UUID = UUID.randomUUID(),
    val behandlingId: UUID,
    val behandlingssteg: Behandlingssteg,
    val behandlingsstegsstatus: Behandlingsstegstatus,
    @Column("ventearsak")
    val venteårsak: Venteårsak? = null,
    val tidsfrist: LocalDate? = null,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
