package no.nav.familie.tilbake.domain.tbd

import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*

data class GrupperingFaktaFeilutbetaling(@Id
                                         val id: UUID = UUID.randomUUID(),
                                         val behandlingId: UUID,
                                         val faktaFeilutbetalingId: UUID,
                                         val aktiv: Boolean = true,
                                         @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                                         val sporbar: Sporbar = Sporbar())