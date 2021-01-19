package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*

data class GrupperingVurdertForeldelse(@Id
                                       val id: UUID = UUID.randomUUID(),
                                       val vurdertForeldelseId: UUID,
                                       val behandlingId: UUID,
                                       val aktiv: Boolean = true,
                                       @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                       val sporbar: Sporbar = Sporbar())