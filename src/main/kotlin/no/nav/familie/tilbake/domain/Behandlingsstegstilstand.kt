package no.nav.familie.tilbake.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*

data class Behandlingsstegstilstand(@Id
                                    val id: UUID = UUID.randomUUID(),
                                    val behandlingId: UUID,
                                    val behandlingsstegstypeId: UUID,
                                    val behandlingsstegsstatus: String,
                                    val versjon: Int = 0,
                                    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                    val sporbar: Sporbar = Sporbar())