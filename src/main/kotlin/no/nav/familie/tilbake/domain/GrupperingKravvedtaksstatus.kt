package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*

data class GrupperingKravvedtaksstatus(@Id
                                       val id: UUID = UUID.randomUUID(),
                                       val kravvedtaksstatus437Id: UUID,
                                       val behandlingId: UUID,
                                       val aktiv: Boolean = true,
                                       @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                       val sporbar: Sporbar = Sporbar())