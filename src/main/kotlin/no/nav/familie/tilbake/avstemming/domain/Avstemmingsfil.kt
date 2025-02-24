package no.nav.familie.tilbake.avstemming.domain

import no.nav.familie.tilbake.common.repository.Sporbar
import no.nav.familie.tilbake.kontrakter.Fil
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Embedded
import java.util.UUID

data class Avstemmingsfil(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val fil: Fil,
    @Version
    val versjon: Long = 0,
    @Embedded(onEmpty = Embedded.OnEmpty.USE_EMPTY)
    val sporbar: Sporbar = Sporbar(),
)
