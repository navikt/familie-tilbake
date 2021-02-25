package no.nav.familie.tilbake.domain.tbd

import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import org.springframework.data.relational.core.mapping.MappedCollection
import java.util.*

data class VurdertForeldelse(@Id
                             val id: UUID = UUID.randomUUID(),
                             val behandlingId: UUID,
                             val aktiv: Boolean = true,
                             @MappedCollection(idColumn = "vurdert_foreldelse_id")
                             val foreldelsesperioder: Set<Foreldelsesperiode> = setOf(),
                             @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                             val sporbar: Sporbar = Sporbar())