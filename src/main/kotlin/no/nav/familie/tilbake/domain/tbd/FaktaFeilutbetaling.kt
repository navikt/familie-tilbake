package no.nav.familie.tilbake.domain.tbd

import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import java.util.*

data class FaktaFeilutbetaling(@Id
                               val id: UUID = UUID.randomUUID(),
                               val begrunnelse: String?,
                               @MappedCollection(idColumn = "fakta_feilutbetaling_id")
                               val perioder: Set<FaktaFeilutbetalingsperiode> = setOf(),
                               @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                               val sporbar: Sporbar = Sporbar())