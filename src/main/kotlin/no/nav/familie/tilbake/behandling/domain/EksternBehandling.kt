package no.nav.familie.tilbake.behandling.domain

import no.nav.familie.tilbake.common.repository.Sporbar
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Embedded
import java.util.*
import javax.persistence.Version

data class EksternBehandling(@Id
                             val id: UUID = UUID.randomUUID(),
                             val eksternId: String,
                             val aktiv: Boolean = true,
                             @Version
                             val versjon: Int = 0,
                             @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
                             val sporbar: Sporbar = Sporbar())
